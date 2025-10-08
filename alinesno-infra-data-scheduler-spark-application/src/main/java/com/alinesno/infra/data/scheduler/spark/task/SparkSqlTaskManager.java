package com.alinesno.infra.data.scheduler.spark.task;

import com.alinesno.infra.data.scheduler.spark.config.SparkConfig;
import com.alinesno.infra.data.scheduler.spark.config.SparkProperties;
import com.alinesno.infra.data.scheduler.spark.model.TaskMeta;
import com.alinesno.infra.data.scheduler.spark.model.TaskResult;
import com.alinesno.infra.data.scheduler.spark.model.TaskStatus;
import com.alinesno.infra.data.scheduler.spark.utils.OssSqlStorage;
import com.alinesno.infra.data.scheduler.spark.utils.StorageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.launcher.SparkLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
public class SparkSqlTaskManager {

    private final ThreadPoolTaskExecutor taskExecutor;
    private final Map<String, Future<TaskResult>> futures = new ConcurrentHashMap<>();
    private final Path tasksDir;
    private final Path resultsDir;

    private final OssSqlStorage ossSqlStorage;

    // 并发限制（正在运行的任务数）
    private final Semaphore concurrencyLimiter;

    private final SparkProperties sparkProperties;

    @Autowired
    public SparkSqlTaskManager(ThreadPoolTaskExecutor taskExecutor, SparkProperties sparkProperties) throws IOException {
        this.taskExecutor = taskExecutor;
        this.sparkProperties = sparkProperties;
        this.tasksDir = StorageUtils.tasksDir();
        this.resultsDir = StorageUtils.resultsDir();

        int permits = 4;
        String env = System.getenv("MAX_CONCURRENT_TASKS");
        if (env != null) {
            try {
                permits = Math.max(1, Integer.parseInt(env.trim()));
            } catch (NumberFormatException ignored) { /* fallback */ }
        } else {
            try {
                int maxPool = taskExecutor.getMaxPoolSize();
                if (maxPool > 0) permits = maxPool;
                else {
                    int corePool = taskExecutor.getCorePoolSize();
                    if (corePool > 0) permits = corePool;
                }
            } catch (Exception ignored) { /* fallback */ }
        }
        this.concurrencyLimiter = new Semaphore(permits);
        log.info("TaskManager concurrency permits = {}", permits);

        // 如果开启上传 sql 到 oss，则初始化 OssSqlStorage
        if (sparkProperties.isUploadSqlToOss()) {
            this.ossSqlStorage = new OssSqlStorage(
                    sparkProperties.getOss().getEndpoint(),
                    sparkProperties.getOss().getBucketName(),
                    sparkProperties.getOss().getAccessKeyId(),
                    sparkProperties.getOss().getAccessKeySecret(),
                    sparkProperties.getOssBasePath()
            );
        } else {
            this.ossSqlStorage = null;
        }
    }

    /**
     * 提交 SQL 任务。返回生成的 taskId（异步执行）。
     *
     * 执行策略：
     * - 创建 task 元信息文件，状态 PENDING；
     * - 异步提交 SparkLauncher 任务，传入 --sql "<sql>" 或 --sql-file <url> --output <resultsDir/taskId> --format csv --appName <appName>
     * - 捕获子进程 stdout/stderr，并在完成后将结果路径或 stdout 保存到 meta。若上传了 SQL 且可以安全删除，则删除 OSS 上的临时对象，否则保留以便后续清理。
     */
    public String submit(String reqSql) throws IOException {
        String taskId = UUID.randomUUID().toString();
        TaskMeta meta = new TaskMeta(taskId, reqSql, TaskStatus.PENDING, Instant.now(), null, null, null, null);
        StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);

        Future<TaskResult> future = taskExecutor.submit(() -> {
            boolean permitAcquired = false;
            try {
                concurrencyLimiter.acquire();
                permitAcquired = true;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                meta.setStatus(TaskStatus.FAILED);
                meta.setErrorMessage("Interrupted while waiting for execution permit");
                meta.setFinishedAt(Instant.now());
                StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);
                throw ie;
            }

            meta.setStatus(TaskStatus.RUNNING);
            meta.setStartedAt(Instant.now());
            StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);

            String poolName = System.getenv("SPARK_SCHEDULER_POOL");
            String appName = "spark-sql-task-" + taskId;

            // 准备结果路径（外部作业可能会产生 csv 目录或其它文件）
            Path resultPathBase = resultsDir.resolve(taskId);
            Files.createDirectories(resultsDir);
            Files.createDirectories(resultPathBase);

            // 构建 SparkLauncher
            SparkLauncher launcher = new SparkLauncher()
                    .setAppResource(sparkProperties.getExecutorJar().getSparkSqlJobJar())
                    .setMainClass("com.alinesno.infra.datascheduler.spark.SparkSqlJob")
                    .setSparkHome(sparkProperties.getSparkHome())
                    .setAppName(appName);

            // master / deployMode / confs
            if (sparkProperties.getMaster() != null){
                launcher.setMaster(sparkProperties.getMaster());
            }

            SparkConfig.sparkSession(launcher, sparkProperties);

            // 在提交前准备 sql 参数（支持上传到 OSS 并使用 --sql-file）
            String sqlArgName = "--sql";
            String sqlArgValue = reqSql;
            String uploadedObjectKey = null; // 用于后续删除或持久化记录

            if (sparkProperties.isUploadSqlToOss() && ossSqlStorage != null) {
                if (reqSql != null && (reqSql.startsWith("http://") || reqSql.startsWith("https://"))) {
                    sqlArgName = "--sql-file";
                    sqlArgValue = reqSql;
                } else {
                    try {
                        OssSqlStorage.UploadResult up = ossSqlStorage.uploadStringAsTempFile(reqSql, Duration.ofHours(1));
                        sqlArgName = "--sql-file";
                        sqlArgValue = up.getPresignedUrl().toString();
                        uploadedObjectKey = up.getObjectKey();
                        log.info("Uploaded SQL to OSS. presignedUrl={} objectKey={}", sqlArgValue, uploadedObjectKey);

                        // 把 objectKey 保存在 meta 中（需在 TaskMeta 中新增 uploadedSqlObjectKey 字段）
                        try {
                            meta.setUploadedSqlObjectKey(uploadedObjectKey);
                        } catch (Throwable t) {
                            log.warn("Failed to set uploadedSqlObjectKey on meta (field may be missing): {}", t.getMessage());
                        }
                        StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);

                    } catch (Exception e) {
                        log.error("Failed to upload SQL to OSS, fallback to pass SQL directly: {}", e.getMessage(), e);
                        sqlArgName = "--sql";
                        sqlArgValue = reqSql;
                        uploadedObjectKey = null;
                    }
                }
            }

            // 构造 appArgs
            List<String> appArgs = new ArrayList<>();
            appArgs.add(sqlArgName);
            appArgs.add(sqlArgValue);
            appArgs.add("--output");
            appArgs.add(resultPathBase.toString()); // 让外部作业写到 resultsDir/taskId (csv 会是目录)
            appArgs.add("--format");
            appArgs.add("csv"); // 以 csv 写出，便于后续读取
            appArgs.add("--appName");
            appArgs.add(appName);

            // 将 args 添加到 launcher
            launcher.addAppArgs(appArgs.toArray(new String[0]));

            Process proc = null;
            StringBuilder stdoutBuf = new StringBuilder();
            StringBuilder stderrBuf = new StringBuilder();
            int exitCode = -1;
            try {
                log.info("Submitting spark job for task {} with appName={}", taskId, appName);
                proc = launcher.launch();

                // 启动线程读取 stdout/stderr
                Thread outReader = streamReaderThread(proc.getInputStream(), stdoutBuf, "stdout-" + taskId);
                Thread errReader = streamReaderThread(proc.getErrorStream(), stderrBuf, "stderr-" + taskId);
                outReader.start();
                errReader.start();

                // 等待进程结束
                exitCode = proc.waitFor();
                outReader.join(5000);
                errReader.join(5000);

                log.info("Spark job process exited for task {} with code {}", taskId, exitCode);

                // 判断外部写出的结果：CSV 会写成目录或文件
                Path csvDir = resultPathBase; // spark csv 写为目录
                if (Files.exists(csvDir) && Files.list(csvDir).findAny().isPresent()) {
                    // 设置为目录或文件
                    meta.setResultPath(csvDir.toString());
                    // 不能轻易判断行数；由调用方自行读取或下载。
                    meta.setRowCount(null);
                    meta.setStatus(TaskStatus.SUCCESS);
                    meta.setFinishedAt(Instant.now());
                    StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);

                    // 成功后尝试删除上传的 SQL（如果安全）
                    tryDeleteUploadedSqlIfSafe(uploadedObjectKey, meta);

                    return new TaskResult(true, meta.getResultPath(), null);
                } else {
                    // 外部作业没有写出文件，尝试把 stdout 保存为结果（例如 SparkSqlJob 未指定输出时会在 stdout 打印 show()）
                    Path stdoutFile = resultsDir.resolve(taskId + ".stdout.log");
                    Files.write(stdoutFile, stdoutBuf.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    meta.setResultPath(stdoutFile.toString());

                    if (exitCode == 0) {
                        meta.setStatus(TaskStatus.SUCCESS);
                        meta.setFinishedAt(Instant.now());
                        StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);

                        // 成功后尝试删除上传的 SQL（如果安全）
                        tryDeleteUploadedSqlIfSafe(uploadedObjectKey, meta);

                        return new TaskResult(true, meta.getResultPath(), null);
                    } else {
                        // 失败：把 stderr 或 stdout 的摘要记录到 meta.errorMessage（长度受限）
                        String errMsg = stderrBuf.length() > 0 ? stderrBuf.toString() : stdoutBuf.toString();
//                        if (errMsg.length() > 10000) errMsg = errMsg.substring(0, 10000) + "...";
                        meta.setStatus(TaskStatus.FAILED);
                        meta.setErrorMessage("exitCode=" + exitCode + " stderr/summary: " + errMsg);
                        meta.setFinishedAt(Instant.now());
                        StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);
                        log.error("Task {} failed. stderr: {}", taskId, stderrBuf.toString());

                        // 任务失败后也尝试删除上传的 SQL（如果安全）
                        tryDeleteUploadedSqlIfSafe(uploadedObjectKey, meta);

                        return new TaskResult(false, meta.getResultPath(), meta.getErrorMessage());
                    }
                }
            } catch (Exception ex) {
                String err = ex.getMessage();
                if (err == null) err = ex.toString();
                meta.setStatus(TaskStatus.FAILED);
                meta.setErrorMessage(err);
                meta.setFinishedAt(Instant.now());
                try {
                    StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);
                } catch (Exception e2) {
                    log.warn("Failed to save meta after exception: {}", e2.getMessage());
                }
                log.error("Exception when running spark job for task {}: {}", taskId, ex.getMessage(), ex);

                // 异常也尝试删除上传的 SQL（如果安全）
                tryDeleteUploadedSqlIfSafe(uploadedObjectKey, meta);

                throw ex;
            } finally {
                if (permitAcquired) concurrencyLimiter.release();
                if (proc != null && proc.isAlive()) {
                    try { proc.destroyForcibly(); } catch (Exception ignored) {}
                }
            }
        });

        futures.put(taskId, future);
        return taskId;
    }

    /**
     * 尝试删除已上传到 OSS 的 SQL 对象（仅在判断为安全时删除），并更新 meta 状态。
     */
    private void tryDeleteUploadedSqlIfSafe(String uploadedObjectKey, TaskMeta meta) {
        if (uploadedObjectKey == null || uploadedObjectKey.isEmpty()) return;
        try {
            if (isSafeToDeleteUploadedSql(sparkProperties)) {
                if (ossSqlStorage != null) {
                    ossSqlStorage.deleteObject(uploadedObjectKey);
                    log.info("Deleted uploaded SQL object {}", uploadedObjectKey);
                    // 从 meta 中移除标记（若 TaskMeta 支持）
                    try {
                        meta.setUploadedSqlObjectKey(null);
                        StorageUtils.saveJson(tasksDir.resolve(meta.getId() + ".json"), meta);
                    } catch (Throwable t) {
                        log.warn("Failed to clear uploadedSqlObjectKey in meta: {}", t.getMessage());
                    }
                }
            } else {
                log.info("Not safe to delete uploaded SQL object {} now; leave it for later cleanup", uploadedObjectKey);
            }
        } catch (Exception ex) {
            log.warn("Failed to delete uploaded SQL object {}: {}", uploadedObjectKey, ex.getMessage());
        }
    }

    /**
     * 简单的安全删除判断：
     * - 若 deployMode 显示为 "client" 或 master 是 local*，认为提交端会负责下载，可以立即删除；
     * - 否则认为可能为 cluster 模式（例如 yarn cluster）不要立即删除，应由后续清理任务处理。
     *
     * 你可以根据实际环境加强判断逻辑（例如显式配置一个属性 forceDeleteUploadedSqlWhenDone=true）。
     */
    private boolean isSafeToDeleteUploadedSql(SparkProperties props) {
        if (props == null) return false;
        String deployMode = props.getDeployMode();
        if (deployMode != null && "client".equalsIgnoreCase(deployMode.trim())) return true;
        String master = props.getMaster();
        if (master != null) {
            String m = master.trim().toLowerCase();
            if (m.startsWith("local")) return true;
            // 如果是 yarn client 模式，上传的临时文件可以由提交端处理，但若是 yarn cluster 则不安全。
            if (m.startsWith("yarn")) {
                if (deployMode != null && "client".equalsIgnoreCase(deployMode.trim())) return true;
                return false;
            }
            // 其它 master 类型（spark://、mesos:// 等）默认不删除以保险起见
        }
        return false;
    }

    /**
     * 辅助：启动线程持续读取输入流到 StringBuilder
     */
    private Thread streamReaderThread(InputStream in, StringBuilder sink, String name) {
        return new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sink.append(line).append('\n');
                }
            } catch (IOException e) {
                log.warn("Error reading stream for {}: {}", name, e.getMessage());
            }
        }, "stream-reader-" + name);
    }

    public Optional<TaskMeta> getMeta(String taskId) throws IOException {
        Path p = tasksDir.resolve(taskId + ".json");
        if (Files.exists(p)) {
            TaskMeta meta = StorageUtils.readJson(p, TaskMeta.class);
            return Optional.of(meta);
        }
        return Optional.empty();
    }
}
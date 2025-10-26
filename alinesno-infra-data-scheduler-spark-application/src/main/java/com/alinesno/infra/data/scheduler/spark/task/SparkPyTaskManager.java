package com.alinesno.infra.data.scheduler.spark.task;

import com.alinesno.infra.data.scheduler.spark.config.SparkProperties;
import com.alinesno.infra.data.scheduler.spark.model.TaskMeta;
import com.alinesno.infra.data.scheduler.spark.model.TaskResult;
import com.alinesno.infra.data.scheduler.spark.model.TaskStatus;
import com.alinesno.infra.data.scheduler.spark.utils.StorageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class SparkPyTaskManager {

    private final ThreadPoolTaskExecutor taskExecutor;
    private final Map<String, Future<TaskResult>> futures = new ConcurrentHashMap<>();
    private final Path tasksDir;
    private final Path resultsDir;
    private final Semaphore concurrencyLimiter;
    private final SparkProperties sparkProperties;

    @Autowired
    public SparkPyTaskManager(ThreadPoolTaskExecutor taskExecutor, SparkProperties sparkProperties) throws IOException {
        this.taskExecutor = taskExecutor;
        this.sparkProperties = sparkProperties;
        this.tasksDir = StorageUtils.tasksDir();
        this.resultsDir = StorageUtils.resultsDir();

        int permits = 4;
        String env = System.getenv("MAX_CONCURRENT_TASKS");
        if (env != null) {
            try { permits = Math.max(1, Integer.parseInt(env.trim())); } catch (Exception ignored) {}
        } else {
            try {
                int maxPool = taskExecutor.getMaxPoolSize();
                if (maxPool > 0) permits = maxPool;
                else {
                    int corePool = taskExecutor.getCorePoolSize();
                    if (corePool > 0) permits = corePool;
                }
            } catch (Exception ignored) {}
        }
        this.concurrencyLimiter = new Semaphore(permits);
        log.info("SparkPyTaskManager concurrency permits = {}", permits);
    }

    /**
     * 提交一个 PySpark 脚本。payload 可以是：
     * - 脚本内容（包含换行或以 import/def 开始） -> 将写为临时 .py 并提交
     * - HTTP(S) URL -> 下载为临时 .py 并提交
     * - 已存在的本地路径或带协议的路径（hdfs://, s3://, oss:// 等） -> 直接作为 application 传给 spark-submit
     *
     * 返回 taskId（可通过 getMeta 查询状态/resultPath）
     */
    public String submit(String payload) throws IOException {
        String taskId = UUID.randomUUID().toString();
        TaskMeta meta = new TaskMeta(taskId, payload, TaskStatus.PENDING, Instant.now(), null, null, null, null);
        StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);

        Future<TaskResult> future = taskExecutor.submit(() -> {
            boolean permitAcquired = false;
            List<Path> tempFiles = new ArrayList<>();
            Process proc = null;
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

            // result path base
            Path resultPathBase = resultsDir.resolve(taskId);
            Files.createDirectories(resultsDir);
            Files.createDirectories(resultPathBase);

            // Resolve spark-submit executable
            String sparkSubmit = resolveSparkSubmitPath();

            // Determine application (python script) path
            String appPath = resolvePythonApp(payload, taskId, tempFiles);

            // Validate local file if it is a local path (skip validation for HDFS/S3/OSS schema URIs)
            if (isLocalPath(appPath)) {
                Path appP = Paths.get(appPath);
                if (!Files.exists(appP)) {
                    throw new IOException("Python application file not found: " + appPath);
                }
            }

            // Build command
            List<String> cmd = new ArrayList<>();
            cmd.add(sparkSubmit);

            // master & deploy-mode
            if (sparkProperties.getMaster() != null && !sparkProperties.getMaster().trim().isEmpty()) {
                cmd.add("--master"); cmd.add(sparkProperties.getMaster());
            }
            if (sparkProperties.getDeployMode() != null && !sparkProperties.getDeployMode().trim().isEmpty()) {
                cmd.add("--deploy-mode"); cmd.add(sparkProperties.getDeployMode());
            }

            // executor settings
            SparkProperties.Executor exec = sparkProperties.getExecutor();
            if (exec != null) {
                if (exec.getCores() > 0) {
                    cmd.add("--executor-cores"); cmd.add(String.valueOf(exec.getCores()));
                }
                if (exec.getMemory() != null && !exec.getMemory().trim().isEmpty()) {
                    cmd.add("--executor-memory"); cmd.add(exec.getMemory());
                }
                if (exec.getInstances() > 0) {
                    cmd.add("--num-executors"); cmd.add(String.valueOf(exec.getInstances()));
                }
                if (exec.getMemoryOverhead() != null && !exec.getMemoryOverhead().trim().isEmpty()) {
                    cmd.add("--conf"); cmd.add("spark.executor.memoryOverhead=" + exec.getMemoryOverhead());
                }
            }

            // common confs
            Map<String, String> confs = buildConfsFromProperties();
            confs.forEach((k, v) -> {
                if (v != null) {
                    cmd.add("--conf");
                    cmd.add(k + "=" + v);
                }
            });

            // Add python application path (the main resource)
            cmd.add(appPath);

            // Optionally: pass a result dir param so script can write outputs; keep as --result-dir
            cmd.add("--result-dir");
            cmd.add(resultPathBase.toAbsolutePath().toString());

            // Start process
            StringBuilder stdoutBuf = new StringBuilder();
            StringBuilder stderrBuf = new StringBuilder();
            int exitCode = -1;
            AtomicBoolean killed = new AtomicBoolean(false);

            try {
                log.info("Launching pyspark task {} with command: {}", taskId, String.join(" ", cmd));
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(false);

                proc = pb.start();

                Thread outReader = streamReaderThread(proc.getInputStream(), stdoutBuf, "stdout-" + taskId);
                Thread errReader = streamReaderThread(proc.getErrorStream(), stderrBuf, "stderr-" + taskId);
                outReader.start();
                errReader.start();

                exitCode = proc.waitFor();
                outReader.join(5000);
                errReader.join(5000);
                log.info("pyspark process exited for task {} with code {}", taskId, exitCode);

                // if result dir contains files -> success
                if (Files.exists(resultPathBase) && Files.list(resultPathBase).findAny().isPresent()) {
                    meta.setResultPath(resultPathBase.toString());
                    meta.setRowCount(null);
                    meta.setStatus(TaskStatus.SUCCESS);
                    meta.setFinishedAt(Instant.now());
                    StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);

                    // cleanup temp scripts if safe
                    cleanupTempFiles(tempFiles);
                    return new TaskResult(true, meta.getResultPath(), null);
                } else {
                    // write stdout to file for user's download/debug
                    Path stdoutFile = resultsDir.resolve(taskId + ".stdout.log");
                    Files.write(stdoutFile, stdoutBuf.toString().getBytes(StandardCharsets.UTF_8),
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                    meta.setResultPath(stdoutFile.toString());

                    if (exitCode == 0) {
                        meta.setStatus(TaskStatus.SUCCESS);
                        meta.setFinishedAt(Instant.now());
                        StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);
                        cleanupTempFiles(tempFiles);
                        return new TaskResult(true, meta.getResultPath(), null);
                    } else {
                        String errMsg = stderrBuf.length() > 0 ? stderrBuf.toString() : stdoutBuf.toString();
                        meta.setStatus(TaskStatus.FAILED);
                        meta.setErrorMessage("exitCode=" + exitCode + " stderr/summary: " + (errMsg != null ? summarize(errMsg, 4096) : ""));
                        meta.setFinishedAt(Instant.now());
                        StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);
                        log.error("Task {} failed. stderr: {}", taskId, stderrBuf.toString());
                        cleanupTempFiles(tempFiles);
                        return new TaskResult(false, meta.getResultPath(), meta.getErrorMessage());
                    }
                }
            } catch (Exception ex) {
                String err = ex.getMessage() == null ? ex.toString() : ex.getMessage();
                meta.setStatus(TaskStatus.FAILED);
                meta.setErrorMessage(err);
                meta.setFinishedAt(Instant.now());
                try { StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta); } catch (Exception e2) { log.warn("Save meta fail: {}", e2.getMessage()); }
                log.error("Exception running pyspark job for task {}: {}", taskId, err, ex);
                cleanupTempFiles(tempFiles);
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

    // 判断是否为本地文件路径（并非 hdfs:// s3:// oss:// 等带协议的路径）
    private boolean isLocalPath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return !(lower.startsWith("hdfs://") || lower.startsWith("s3://") || lower.startsWith("s3a://")
                || lower.startsWith("oss://") || lower.startsWith("http://") || lower.startsWith("https://"));
    }

    // 把 payload 解析为应用脚本路径；并把临时文件记录到 tempFiles 列表以便后续清理
    private String resolvePythonApp(String payload, String taskId, List<Path> tempFiles) throws IOException {
        if (payload == null) throw new IOException("Empty payload for python app");
        String trimmed = payload.trim();

        // HTTP(S) -> 下载为临时文件
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            Path tmp = Files.createTempFile("pyspark-down-", "-" + taskId + ".py");
            try (InputStream in = new URL(trimmed).openStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            tempFiles.add(tmp);
            log.info("Downloaded remote python script to {}", tmp);
            return tmp.toAbsolutePath().toString();
        }

        // 如果看起来像脚本内容（包含换行或以 import/def/class 开头），写入 tasksDir/scripts/<taskId>.py
        if (looksLikeScriptContent(trimmed)) {
            Path scriptDir = tasksDir.resolve("scripts");
            Files.createDirectories(scriptDir);
            Path tmp = scriptDir.resolve(taskId + ".py");
            Files.write(tmp, trimmed.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            tempFiles.add(tmp);
            log.info("Wrote inline python script to {}", tmp);
            return tmp.toAbsolutePath().toString();
        }

        // 其他：视为用户提供的 path（本地路径或带协议的路径）
        return trimmed;
    }

    private boolean looksLikeScriptContent(String s) {
        if (s.contains("\n")) return true;
        String low = s.toLowerCase();
        return low.startsWith("import ") || low.startsWith("from ") || low.startsWith("def ") || low.startsWith("class ");
    }

    private void cleanupTempFiles(List<Path> tempFiles) {
        if (tempFiles == null || tempFiles.isEmpty()) return;
        boolean safeToDelete = isSafeToDeleteTempScripts(sparkProperties);
        if (!safeToDelete) {
            log.info("Not safe to delete temporary script files; keeping: {}", tempFiles);
            return;
        }
        for (Path p : tempFiles) {
            try {
                Files.deleteIfExists(p);
                log.info("Deleted temporary script file {}", p);
            } catch (Exception e) {
                log.warn("Failed to delete temp script {}: {}", p, e.getMessage());
            }
        }
    }

    // 简化的安全判断：client / local 模式可以删除本地临时脚本；yarn cluster 模式不删除
    private boolean isSafeToDeleteTempScripts(SparkProperties props) {
        if (props == null) return false;
        String deployMode = props.getDeployMode();
        if (deployMode != null && "client".equalsIgnoreCase(deployMode.trim())) return true;
        String master = props.getMaster();
        if (master != null) {
            String m = master.trim().toLowerCase();
            if (m.startsWith("local")) return true;
            if (m.startsWith("yarn")) {
                // yarn client -> safe, yarn cluster -> not safe
                return deployMode != null && "client".equalsIgnoreCase(deployMode.trim());
            }
        }
        return false;
    }

    private String resolveSparkSubmitPath() {
        // priority: spark.pyAppPath system prop? no. use SPARK_SUBMIT_PATH env -> SPARK_HOME -> "spark-submit"
        String configured = System.getenv("SPARK_SUBMIT_PATH");
        if (configured != null && !configured.trim().isEmpty()) return configured;
        String sparkHome = sparkProperties.getSparkHome();
        if (sparkHome != null && !sparkHome.trim().isEmpty()) {
            return Paths.get(sparkHome, "bin", "spark-submit").toString();
        }
        return "spark-submit";
    }

    private Map<String, String> buildConfsFromProperties() {
        Map<String, String> confs = new LinkedHashMap<>();
        // sql-ish properties used as spark confs (kept as useful defaults)
        if (sparkProperties.getSql() != null) {
            if (sparkProperties.getSql().getWarehouseDir() != null) {
                confs.put("spark.sql.warehouse.dir", sparkProperties.getSql().getWarehouseDir());
            }
            confs.put("spark.sql.shuffle.partitions", String.valueOf(sparkProperties.getSql().getShufflePartitions()));
            confs.put("spark.sql.adaptive.enabled", String.valueOf(sparkProperties.getSql().isAdaptiveEnabled()));
            if (sparkProperties.getSql().getAdaptiveTargetPostShuffleInputSize() != null) {
                confs.put("spark.sql.adaptive.targetPostShuffleInputSize", sparkProperties.getSql().getAdaptiveTargetPostShuffleInputSize());
            }
        }
        if (sparkProperties.getDriver() != null && sparkProperties.getDriver().getBindAddress() != null) {
            confs.put("spark.driver.bindAddress", sparkProperties.getDriver().getBindAddress());
        }
        confs.put("spark.default.parallelism", String.valueOf(sparkProperties.getDefaultParallelism()));

        SparkProperties.DynamicAllocation da = sparkProperties.getDynamicAllocation();
        if (da != null) {
            confs.put("spark.dynamicAllocation.enabled", String.valueOf(da.isEnabled()));
            confs.put("spark.dynamicAllocation.minExecutors", String.valueOf(da.getMinExecutors()));
            confs.put("spark.dynamicAllocation.maxExecutors", String.valueOf(da.getMaxExecutors()));
            if (da.getExecutorIdleTimeout() != null) {
                confs.put("spark.dynamicAllocation.executorIdleTimeout", da.getExecutorIdleTimeout());
            }
        }
        return confs;
    }

    private Thread streamReaderThread(InputStream in, StringBuilder sink, String name) {
        return new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sink.append(line).append('\n');
                }
            } catch (IOException e) {
                log.warn("Error reading stream {}: {}", name, e.getMessage());
            }
        }, "stream-reader-" + name);
    }

    private String summarize(String s, int maxLen) {
        if (s == null) return null;
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...(truncated)";
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
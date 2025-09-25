package com.alinesno.infra.data.scheduler.spark.task;

import com.alinesno.infra.data.scheduler.spark.model.TaskMeta;
import com.alinesno.infra.data.scheduler.spark.model.TaskResult;
import com.alinesno.infra.data.scheduler.spark.model.TaskStatus;
import com.alinesno.infra.data.scheduler.spark.utils.StorageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Service
public class TaskManager {

    private final ExecutorService executor;
    private final Map<String, Future<TaskResult>> futures = new ConcurrentHashMap<>();
    private final Path tasksDir;
    private final Path resultsDir;
    private final SparkSession spark;

    public TaskManager(SparkSession spark) throws IOException {
        this.spark = spark;
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2); // configurable
        this.executor = Executors.newFixedThreadPool(threads);
        this.tasksDir = StorageUtils.tasksDir();
        this.resultsDir = StorageUtils.resultsDir();
        // load persisted tasks if needed (optional)
    }

    public String submit(String reqSql) throws IOException {
        String taskId = UUID.randomUUID().toString();
        TaskMeta meta = new TaskMeta(taskId, reqSql, TaskStatus.PENDING, Instant.now(), null, null, null, null);
        StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);

        // TaskManager.java 核心修改（替换原 submit 方法中的 Callable 逻辑）
        Future<TaskResult> future = executor.submit(() -> {
            // 更新 meta 为 RUNNING
            meta.setStatus(TaskStatus.RUNNING);
            meta.setStartedAt(Instant.now());
            StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);

            String jobGroup = "task-" + taskId;
            spark.sparkContext().setJobGroup(jobGroup, "SQL task " + taskId, true);
            long start = System.currentTimeMillis();
            try {
                // 关键修改：拆分 SQL 语句（按分号拆分，过滤空语句）
                String[] rawStmts = reqSql.split(";"); // reqSql 是传入的 SQL 字符串
                List<String> stmts = new ArrayList<>();
                for (String stmt : rawStmts) {
                    String trimmed = stmt.trim();
                    if (!trimmed.isEmpty()) {
                        stmts.add(trimmed);
                    }
                }

                // 逐条执行拆分后的语句
                Dataset<Row> lastResult = null; // 记录最后一条语句的结果（如 SELECT）
                for (String stmt : stmts) {
                    // 打印执行日志（方便调试）
                    log.debug("Executing SQL stmt: {}", stmt);
                    // 执行单条语句
                    lastResult = spark.sql(stmt);
                }

                // 处理结果（沿用之前的逻辑，控制返回大小）
                long maxReturnRows = 10000;
                if (lastResult != null) {
                    long countEstimate = lastResult.limit((int) maxReturnRows + 1).count();
                    Path resultFile = resultsDir.resolve(taskId + ".csv");

                    if (countEstimate > maxReturnRows) {
                        // 结果过大，写入文件
                        lastResult.write().mode("overwrite").option("header", "true").csv(resultFile.toString());
                        meta.setResultPath(resultFile.toString());
                    } else {
                        // 小结果，写入 GZIP JSON
                        List<Row> rows = lastResult.collectAsList();
                        List<Map<String, Object>> data = new ArrayList<>();
                        String[] cols = lastResult.columns();
                        for (Row r : rows) {
                            Map<String, Object> map = new LinkedHashMap<>();
                            for (int i = 0; i < cols.length; i++) {
                                map.put(cols[i], r.get(i));
                            }
                            data.add(map);
                        }
                        Path smallJson = resultsDir.resolve(taskId + ".json.gz");
                        StorageUtils.saveGzipJson(smallJson, data); // 使用优化的 GZIP 写入
                        meta.setResultPath(smallJson.toString());
                    }
                    meta.setRowCount(countEstimate);
                }

                // 更新成功状态
                meta.setStatus(TaskStatus.SUCCESS);
                meta.setFinishedAt(Instant.now());
                StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);
                return new TaskResult(true, meta.getResultPath(), null);

            } catch (Exception ex) {
                // 捕获异常并更新失败状态
                meta.setStatus(TaskStatus.FAILED);
                meta.setErrorMessage(ex.getMessage());
                meta.setFinishedAt(Instant.now());
                StorageUtils.saveJson(tasksDir.resolve(taskId + ".json"), meta);
                throw ex;
            } finally {
                spark.sparkContext().clearJobGroup();
            }
        });

        futures.put(taskId, future);
        return taskId;
    }

    public Optional<TaskMeta> getMeta(String taskId) throws IOException {
        Path p = tasksDir.resolve(taskId + ".json");
        if (Files.exists(p)) {
            TaskMeta meta = StorageUtils.readJson(p, TaskMeta.class);
            return Optional.of(meta);
        }
        return Optional.empty();
    }

    public boolean cancel(String taskId) {
        Future<TaskResult> f = futures.get(taskId);
        if (f != null) {
            boolean cancelled = f.cancel(true);
            spark.sparkContext().cancelJobGroup("task-" + taskId);
            return cancelled;
        }
        return false;
    }
}
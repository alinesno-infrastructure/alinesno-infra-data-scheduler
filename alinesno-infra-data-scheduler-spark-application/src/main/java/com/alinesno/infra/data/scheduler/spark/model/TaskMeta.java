package com.alinesno.infra.data.scheduler.spark.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * 任务元信息（持久化到磁盘或共享存储）
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskMeta {

    // 唯一任务ID（UUID）
    private String id;

    // 原始 SQL
    private String sql;

    private String uploadedSqlObjectKey ;

    // 任务状态
    private TaskStatus status;

    // 创建/开始/结束时间（使用 Instant）
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant startedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant finishedAt;

    // 结果行数（可为 null）
    private Long rowCount;

    // 结果文件或小结果的路径（例如本地路径、HDFS/OSS 路径或 JSON 文件）
    private String resultPath;

    // 错误信息
    private String errorMessage;

    // 对应的 spark job group（用于 cancel）
    private String jobGroup;

    // 执行时间（毫秒）
    private Long executionTimeMs;

    // 额外扩展字段（可选）
    private Map<String, Object> extras;

    public TaskMeta(String id,
                    String sql,
                    TaskStatus status,
                    Instant createdAt,
                    Instant startedAt,
                    Instant finishedAt,
                    Long rowCount,
                    String resultPath) {
        this.id = id;
        this.sql = sql;
        this.status = status;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.rowCount = rowCount;
        this.resultPath = resultPath;
        // 其余字段保持 null
    }

    public TaskMeta(String id,
                    String sql,
                    TaskStatus status,
                    Instant createdAt,
                    Instant startedAt,
                    Instant finishedAt,
                    Long rowCount,
                    String resultPath,
                    String errorMessage,
                    String jobGroup,
                    Long executionTimeMs,
                    Map<String, Object> extras) {
        this.id = id;
        this.sql = sql;
        this.status = status;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.rowCount = rowCount;
        this.resultPath = resultPath;
        this.errorMessage = errorMessage;
        this.jobGroup = jobGroup;
        this.executionTimeMs = executionTimeMs;
        this.extras = extras;
    }


}
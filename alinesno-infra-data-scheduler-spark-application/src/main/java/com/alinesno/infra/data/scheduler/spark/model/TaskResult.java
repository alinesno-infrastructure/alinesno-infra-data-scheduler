package com.alinesno.infra.data.scheduler.spark.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务执行结果摘要（从执行线程返回给 TaskManager 的结果对象）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskResult {

    // 是否成功
    private boolean success;

    // 若成功，指向结果位置（本地/分布式路径）
    private String resultPath;

    // 错误信息（若失败）
    private String errorMessage;

    // 结果行数（若已统计）
    private long rowCount;

    // 执行时间（毫秒）
    private long executionTimeMs;

    public TaskResult(boolean b, String resultPath, Object o) {
        this.success = b;
        this.resultPath = resultPath;
    }
}
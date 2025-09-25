package com.alinesno.infra.data.scheduler.spark.model;

/**
 * 任务状态
 */
public enum TaskStatus {
    PENDING,    // 已提交，尚未执行
    RUNNING,    // 正在执行
    SUCCESS,    // 成功完成
    FAILED,     // 执行失败
    CANCELLED,  // 被用户取消
    TIMEOUT     // 超时终止
}
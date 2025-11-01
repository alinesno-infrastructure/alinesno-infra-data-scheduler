package com.alinesno.infra.data.scheduler.trigger.bean;

public enum TaskState {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
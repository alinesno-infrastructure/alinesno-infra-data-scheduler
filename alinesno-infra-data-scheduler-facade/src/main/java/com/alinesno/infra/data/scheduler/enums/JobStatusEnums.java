package com.alinesno.infra.data.scheduler.enums;

public enum JobStatusEnums {
    NOT_FOUND,   // Job 不存在
    NO_TRIGGER,  // Job 存在但没有对应的 Trigger
    RUNNING,     // 当前正在执行
    NORMAL,      // 已调度并处于正常状态（可触发）
    PAUSED,      // 被暂停
    COMPLETE,    // Trigger 已完成
    ERROR,       // Trigger 出错
    BLOCKED      // 被阻塞
}
package com.alinesno.infra.data.scheduler.enums;

import lombok.Getter;

/**
 * 异常策略枚举 <br/>
 * 0 -> 继续执行 <br/>
 * 1 -> 停止执行 <br/>
 * 2 -> 暂停任务 <br/>
 * -1 -> 未知
 */
@Getter
public enum ExecutionStrategyEnums {

    IGNORE_FLOW(0, "继续执行"),
    STOP_FLOW(1, "停止执行"),
    PAUSE_TASK(2, "暂停任务"),
    UNKNOWN(-1, "未知");

    private final int code;
    private final String description;

    ExecutionStrategyEnums(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据 code 获取枚举，未知值返回 UNKNOWN
     */
    public static ExecutionStrategyEnums fromCode(int code) {
        return switch (code) {
            case 0 -> IGNORE_FLOW;
            case 1 -> STOP_FLOW;
            case 2 -> PAUSE_TASK;
            default -> UNKNOWN;
        };
    }

    /**
     * 根据 code 获取描述
     */
    public static String descriptionOf(int code) {
        return fromCode(code).getDescription();
    }

    @Override
    public String toString() {
        return description;
    }
}
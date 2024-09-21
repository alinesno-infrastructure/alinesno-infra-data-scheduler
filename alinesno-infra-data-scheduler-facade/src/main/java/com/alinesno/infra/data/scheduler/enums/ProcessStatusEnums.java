package com.alinesno.infra.data.scheduler.enums;

import lombok.Getter;

/**
 * 任务状态, 0-未运行，1-运行中，2-已停止，3-已失败，4-已结束
 */
@Getter
public enum ProcessStatusEnums {

    UNRUN(0,"未运行"),
    RUNNING(1,"运行中"),
    STOP(2,"已停止"),
    FAIL(3,"已失败"),
    END(4,"已结束");

    private final int code;
    private final String desc;

    ProcessStatusEnums(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}

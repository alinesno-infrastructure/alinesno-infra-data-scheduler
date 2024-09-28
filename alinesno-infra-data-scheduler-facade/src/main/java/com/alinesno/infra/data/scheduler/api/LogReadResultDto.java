package com.alinesno.infra.data.scheduler.api;

import lombok.Data;

@Data
public class LogReadResultDto {

    private String log; // 日志文本片段
    private int nextOffset; // 下次读取应该开始的位置
    private boolean hasMoreLog; // 是否还有更多的日志内容，默认为 false

    public LogReadResultDto(String log, int nextOffset) {
        this(log, nextOffset, false); // 默认没有更多的日志内容
    }

    public LogReadResultDto(String log, int nextOffset, boolean hasMoreLog) {
        this.log = log;
        this.nextOffset = nextOffset;
        this.hasMoreLog = hasMoreLog;
    }
}
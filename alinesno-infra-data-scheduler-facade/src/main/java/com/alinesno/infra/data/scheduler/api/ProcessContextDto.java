package com.alinesno.infra.data.scheduler.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

/**
 * 任务执行上下文DTO类
 * 用于在任务调度API中传递任务执行的相关信息
 */
@ToString
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessContextDto {
    // 任务名称，用于标识任务
    private String taskName;

    // 上下文信息，可用于存储任务执行的额外信息
    private String context;

    private String projectCode ; // 项目代码

    private int timeout ; // 超时时间

    // 是否启用警报，用于控制任务执行异常时是否发送警报
    private boolean isAlertEnabled;

    // 监控邮箱，用于接收任务执行警报的电子邮件地址
    private String monitorEmail;

    // Cron表达式，用于定义任务执行的调度时间规则
    private String cronExpression;

    // 任务开始时间，用于指定任务开始执行的时间
    private String startTime;

    // 任务结束时间，用于指定任务结束执行的时间
    private String endTime;
}
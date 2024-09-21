package com.alinesno.infra.data.scheduler.executor.bean;

import lombok.Data;

/**
 * 表示Quartz作业的值对象，用于存储作业的相关信息。
 */
@Data
public class QuartzJobsVO {
    private String jobId; // 作业ID
    private String jobDetailName; // JobDetail的名称
    private String groupName; // JobDetail所属的组名
    private String jobCronExpression; // Job的Cron表达式或者触发器的键
    private String status; // Job或Trigger的状态
}

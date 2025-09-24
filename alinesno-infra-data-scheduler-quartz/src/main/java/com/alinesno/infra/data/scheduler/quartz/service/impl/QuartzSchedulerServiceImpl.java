package com.alinesno.infra.data.scheduler.quartz.service.impl;

import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.enums.JobStatusEnums;
import com.alinesno.infra.data.scheduler.quartz.job.QuartzJob;
import com.alinesno.infra.data.scheduler.scheduler.IQuartzSchedulerService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;

/**
 * 分布式调度任务服务实现类
 */
@Slf4j
@Service
public class QuartzSchedulerServiceImpl implements IQuartzSchedulerService {

    @Autowired
    private Scheduler scheduler ;

    @Override
    public void createCronJob(Long jobId, long jobInstanceId) throws SQLException, IOException {

    }

    @Value("${quartz.default-cron:0 0/5 * * * ?}")  // 从配置读取，默认每5分钟执行一次
    private String defaultCron;

    @SneakyThrows
    @Override
    public void addJob(String jobId, String cron) {
        // 使用配置的默认值
        String effectiveCron = (cron == null || cron.trim().isEmpty()) ? defaultCron : cron;

        JobDetail jobDetail = JobBuilder.newJob(QuartzJob.class)
                .usingJobData(PipeConstants.PROCESS_ID, jobId)
                .withIdentity(jobId, PipeConstants.JOB_GROUP_NAME)
                .build();

        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                .cronSchedule(effectiveCron)
                .withMisfireHandlingInstructionDoNothing();  // 忽略执行失败的任务

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .usingJobData(PipeConstants.PROCESS_ID, jobId)
                .withIdentity(jobId, PipeConstants.TRIGGER_GROUP_NAME)
                .withSchedule(scheduleBuilder)
                .startNow()
                .build();

        scheduler.scheduleJob(jobDetail, trigger);

        log.debug("Quartz 创建了job:{}", jobDetail.getKey());

        // 默认任务是停止的状态
        scheduler.pauseTrigger(TriggerKey.triggerKey(jobId, PipeConstants.TRIGGER_GROUP_NAME));
    }

    /**
     * 更新任务的Cron表达式
     * @param jobId 任务ID
     * @param newCron 新的Cron表达式
     */
    @SneakyThrows
    @Override
    public void updateJobCron(String jobId, String newCron) {
        if (newCron == null || newCron.trim().isEmpty()) {
            log.warn("更新任务Cron失败，jobId:{}，新Cron表达式为空，使用默认值:{}", jobId, defaultCron);
            newCron = defaultCron;
        }

        newCron = newCron.trim();

        // 先校验 cron 表达式是否有效
        if (!CronExpression.isValidExpression(newCron)) {
            String msg = "无效的 Cron 表达式: " + newCron
                    + ". 注意 Quartz 要求 day-of-month 或 day-of-week 其中一个必须为 '?'。示例每分钟: '0 0/1 * * * ?'。";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // 获取触发器键
        TriggerKey triggerKey = TriggerKey.triggerKey(jobId, PipeConstants.TRIGGER_GROUP_NAME);

        // 获取当前触发器
        CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);

        if (trigger == null) {
            log.error("更新任务Cron失败，未找到对应的触发器，jobId:{}", jobId);
            throw new SchedulerException("未找到对应的触发器: " + jobId);
        }

        // 如果Cron表达式没有变化，则不需要更新
        String currentCron = trigger.getCronExpression();
        if (currentCron.equals(newCron)) {
            log.debug("任务Cron表达式未变化，无需更新，jobId:{}", jobId);
            return;
        }

        try {
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder
                    .cronSchedule(newCron)
                    .withMisfireHandlingInstructionDoNothing();  // 忽略执行失败的任务

            trigger = trigger.getTriggerBuilder()
                    .withIdentity(triggerKey)
                    .withSchedule(scheduleBuilder)
                    .build();
            scheduler.rescheduleJob(triggerKey, trigger);

            log.debug("Quartz 更新了job的Cron表达式:{}，新Cron:{}", jobId, newCron);

            scheduler.pauseTrigger(triggerKey);
        } catch (RuntimeException ex) {
            // 捕获 Quartz 内部 ParseException 包装的 RuntimeException，返回更友好的错误
            String msg = "解析 Cron 表达式时出错: " + newCron + "，原因: " + ex.getMessage();
            log.error(msg, ex);
            throw new IllegalArgumentException(msg, ex);
        }
    }

    @SneakyThrows
    @Override
    public JobStatusEnums getJobStatus(String jobId) {
        if (jobId == null || jobId.trim().isEmpty()) {
            return JobStatusEnums.NOT_FOUND;
        }

        JobKey jobKey = JobKey.jobKey(jobId, PipeConstants.JOB_GROUP_NAME);
        // 先判断 Job 是否存在
        if (!scheduler.checkExists(jobKey)) {
            return JobStatusEnums.NOT_FOUND;
        }

        // 判断是否正在执行（优先级高于 Trigger 状态）
        for (JobExecutionContext ctx : scheduler.getCurrentlyExecutingJobs()) {
            if (ctx.getJobDetail().getKey().equals(jobKey)) {
                return JobStatusEnums.RUNNING;
            }
        }

        // 再检查 Trigger 状态（你的实现每个 job 对应一个 trigger）
        TriggerKey triggerKey = TriggerKey.triggerKey(jobId, PipeConstants.TRIGGER_GROUP_NAME);
        if (!scheduler.checkExists(triggerKey)) {
            return JobStatusEnums.NO_TRIGGER;
        }

        Trigger.TriggerState state = scheduler.getTriggerState(triggerKey);
        return switch (state) {
            case NORMAL -> JobStatusEnums.NORMAL;
            case PAUSED -> JobStatusEnums.PAUSED;
            case COMPLETE -> JobStatusEnums.COMPLETE;
            case ERROR -> JobStatusEnums.ERROR;
            case BLOCKED -> JobStatusEnums.BLOCKED;
            default -> JobStatusEnums.NO_TRIGGER;
        };
    }

}

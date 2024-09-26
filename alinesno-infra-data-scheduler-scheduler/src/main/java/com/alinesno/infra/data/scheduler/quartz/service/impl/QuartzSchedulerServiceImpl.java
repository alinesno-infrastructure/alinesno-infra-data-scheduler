package com.alinesno.infra.data.scheduler.quartz.service.impl;

import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.quartz.job.QuartzJob;
import com.alinesno.infra.data.scheduler.scheduler.IQuartzSchedulerService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
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

    @SneakyThrows
    @Override
    public void addJob(String jobId, String cron) {

        JobDetail jobDetail = JobBuilder.newJob(QuartzJob.class)
                .usingJobData(PipeConstants.PROCESS_ID, jobId)
                .withIdentity(jobId , PipeConstants.JOB_GROUP_NAME)
                .build();//执行

        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
        CronTrigger trigger = TriggerBuilder.newTrigger()
                .usingJobData(PipeConstants.PROCESS_ID, jobId)
                .withIdentity(jobId , PipeConstants.TRIGGER_GROUP_NAME)
                .withSchedule(scheduleBuilder)
                .startNow()
                .build();

        scheduler.scheduleJob(jobDetail, trigger);

        log.debug("Quartz 创建了job:{}" , jobDetail.getKey());

        // 默认任务是停止的状态
        scheduler.pauseTrigger(TriggerKey.triggerKey(jobId , PipeConstants.TRIGGER_GROUP_NAME));
    }
}

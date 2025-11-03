package com.alinesno.infra.data.scheduler.trigger.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.common.web.log.utils.SpringUtils;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.trigger.entity.JobEntity;
import com.alinesno.infra.data.scheduler.trigger.entity.TriggerEntity;
import com.alinesno.infra.data.scheduler.trigger.mapper.JobMapper;
import com.alinesno.infra.data.scheduler.trigger.service.BuildQueueService;
import com.alinesno.infra.data.scheduler.trigger.service.IJobService;
import com.alinesno.infra.data.scheduler.trigger.service.ITriggerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * JobServiceImpl
 */
@Slf4j
@Service
public class JobServiceImpl extends IBaseServiceImpl<JobEntity , JobMapper> implements IJobService {

    @Override
    public void createJob(ProcessDefinitionEntity processDefinition) {

        JobEntity job = new JobEntity();

        job.setId(IdUtil.getSnowflakeNextId());
        job.setProcessId(processDefinition.getId());
        job.setName(processDefinition.getName());
        job.setRemark(processDefinition.getDescription());

        save(job);

    }

    @Override
    public void deleteJob(String processId) {

        ITriggerService triggerService = SpringUtils.getBean(ITriggerService.class);
        BuildQueueService buildQueueService = SpringUtils.getBean(BuildQueueService.class);

        // 1.删除定义的job
        LambdaQueryWrapper<JobEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(JobEntity::getProcessId, processId);

        JobEntity job = getOne(wrapper);

        if(job != null){
            // 2. 删除任务
            buildQueueService.cancel(String.valueOf(job.getId()));

            // 3. 删除触发器
            LambdaUpdateWrapper<TriggerEntity> triggerWrapper = new LambdaUpdateWrapper<>();
            triggerWrapper.eq(TriggerEntity::getJobId, job.getId()) ;
            triggerWrapper.eq(TriggerEntity::getProcessId, processId) ;

            triggerService.remove(triggerWrapper) ;

            // 4. 删除任务定义
            removeById(job.getId());
        }

    }

    /**
     * 如果存在则先删除，如果不存在则创建
     * @param processId
     * @param cron
     */
    @Override
    public void updateJobCron(String processId, String cron) {

        ITriggerService triggerService = SpringUtils.getBean(ITriggerService.class);
        BuildQueueService buildQueueService = SpringUtils.getBean(BuildQueueService.class);

        // 1. 查找 job，如不存在则创建最小信息的 job
        LambdaQueryWrapper<JobEntity> jobWrapper = new LambdaQueryWrapper<>();
        jobWrapper.eq(JobEntity::getProcessId, processId);
        JobEntity job = getOne(jobWrapper);

        Assert.notNull(job, "任务没有找到，更新触发器失败.");

        // 2. 找到并删除旧的 trigger（如果有），并尝试取消队列中的构建
        LambdaQueryWrapper<TriggerEntity> triggerQuery = new LambdaQueryWrapper<>();
        triggerQuery.eq(TriggerEntity::getJobId, job.getId());
        triggerQuery.eq(TriggerEntity::getProcessId, processId);

        TriggerEntity existing = triggerService.getOne(triggerQuery);
        if (existing != null) {
            log.info("已找到旧的触发器(id={})，将删除它", existing.getId());
            try {
                // 按照 deleteJob 的做法，先取消队列中的任务（以 jobId 为标识）
                buildQueueService.cancel(String.valueOf(job.getId()));
            } catch (Exception ex) {
                log.error("删除旧触发器时出错：{}", ex.getMessage(), ex);
            }
            // 删除触发器记录
            triggerService.remove(triggerQuery);
        }

        TriggerEntity t = new TriggerEntity();
        t.setId(IdUtil.getSnowflakeNextId());
        t.setJobId(job.getId());
        t.setProcessId(Long.valueOf(processId));
        t.setCron(cron.trim());

        triggerService.save(t);
        log.info("已创建新的触发器(id={})，任务ID={}, cron={}", t.getId(), job.getId(), t.getCron());
    }
}
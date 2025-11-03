package com.alinesno.infra.data.scheduler.trigger.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.trigger.entity.JobEntity;

/**
 * 任务服务接口
 */
public interface IJobService extends IBaseService<JobEntity> {

    /**
     * 创建任务
     * @param processDefinition
     */
    void createJob(ProcessDefinitionEntity processDefinition);

    /**
     * 删除任务
     * @param processId
     */
    void deleteJob(String processId);

    /**
     * 更新任务计划
     * @param processId
     * @param cron
     */
    void updateJobCron(String processId , String cron);
}

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

    /**
     * 暂停任务：通常意味着禁用或删除与该 job 关联的触发器，并尝试取消队列中未开始的构建。
     * @param processId 进程ID（字符串形式）
     */
    void pauseJob(String processId);

    /**
     * 恢复任务：恢复触发器（需要传入 cron 表达式或从持久化中获取原始 cron），使任务再次被调度。
     * @param processId 进程ID（字符串形式）
     * @param cron 恢复时使用的 cron 表达式（如果系统能从 DB 读取原始 cron，可允许为 null）
     */
    void resumeJob(String processId, String cron);
}

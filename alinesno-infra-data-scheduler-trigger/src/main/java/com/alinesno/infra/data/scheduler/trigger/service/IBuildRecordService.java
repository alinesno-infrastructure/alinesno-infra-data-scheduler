package com.alinesno.infra.data.scheduler.trigger.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.trigger.entity.BuildRecordEntity;

import java.util.List;

/**
 * 描述：构建记录服务接口
 */
public interface IBuildRecordService extends IBaseService<BuildRecordEntity> {

    /**
     * 创建一条入队记录（并返回记录实体）
     */
    BuildRecordEntity createQueuedRecord(Long jobId, Long triggerId, String jobName, Integer buildNumber, Long processId);

    /**
     * 标记任务正在运行
     * @param recordId
     * @return
     */
    BuildRecordEntity markRunning(Long recordId);

    /**
     * 标记任务完成
     * @param recordId
     * @param message
     * @return
     */
    BuildRecordEntity markCompleted(Long recordId, String message);

    /**
     * 标记任务失败
     * @param recordId
     * @param message
     * @return
     */
    BuildRecordEntity markFailed(Long recordId, String message);

    /**
     * 标记任务取消
     * @param recordId
     * @param message
     * @return
     */
    BuildRecordEntity markCancelled(Long recordId, String message);

    /**
     * 查询未完成的任务记录（QUEUED 或 RUNNING 状态）
     */
    List<BuildRecordEntity> listUnfinishedRecords();
}
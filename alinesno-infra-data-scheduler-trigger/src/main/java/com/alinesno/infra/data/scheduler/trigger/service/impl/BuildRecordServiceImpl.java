package com.alinesno.infra.data.scheduler.trigger.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.scheduler.trigger.bean.TaskState;
import com.alinesno.infra.data.scheduler.trigger.entity.BuildRecordEntity;
import com.alinesno.infra.data.scheduler.trigger.mapper.BuildRecordMapper;
import com.alinesno.infra.data.scheduler.trigger.service.IBuildRecordService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 构建记录服务实现类
 */
@Slf4j
@Service
public class BuildRecordServiceImpl extends IBaseServiceImpl<BuildRecordEntity , BuildRecordMapper> implements IBuildRecordService {

    @Override
    @Transactional
    public BuildRecordEntity createQueuedRecord(Long jobId, Long triggerId, String jobName, Integer buildNumber, Long processId) {
        BuildRecordEntity rec = new BuildRecordEntity();
        rec.setProcessId(processId);
        rec.setJobId(jobId);
        rec.setTriggerId(triggerId);
        rec.setBuildNumber(buildNumber);
        rec.setDisplayName(jobName + " #" + buildNumber);
        rec.setStatus(TaskState.QUEUED.name());
        rec.setEnqueuedAt(LocalDateTime.now());
        this.save(rec);
        return rec;
    }

    @Override
    @Transactional
    public BuildRecordEntity markRunning(Long recordId) {
        BuildRecordEntity rec = this.getById(recordId);
        if (rec == null) return null;
        rec.setStatus(TaskState.RUNNING.name());
        rec.setStartedAt(LocalDateTime.now());
        this.updateById(rec);
        return rec;
    }

    @Override
    @Transactional
    public BuildRecordEntity markCompleted(Long recordId, String message) {
        BuildRecordEntity rec = this.getById(recordId);
        if (rec == null) return null;
        rec.setStatus(TaskState.COMPLETED.name());
        rec.setMessage(message);
        rec.setFinishedAt(LocalDateTime.now());
        if (rec.getStartedAt() != null) {
            rec.setDurationMs(java.time.Duration.between(rec.getStartedAt(), rec.getFinishedAt()).toMillis());
        }
        this.updateById(rec);
        return rec;
    }

    @Override
    @Transactional
    public BuildRecordEntity markFailed(Long recordId, String message) {
        BuildRecordEntity rec = this.getById(recordId);
        if (rec == null) return null;
        rec.setStatus(TaskState.FAILED.name());
        rec.setMessage(message);
        rec.setFinishedAt(LocalDateTime.now());
        if (rec.getStartedAt() != null) {
            rec.setDurationMs(java.time.Duration.between(rec.getStartedAt(), rec.getFinishedAt()).toMillis());
        }
        this.updateById(rec);
        return rec;
    }

    @Override
    @Transactional
    public BuildRecordEntity markCancelled(Long recordId, String message) {
        BuildRecordEntity rec = this.getById(recordId);
        if (rec == null) return null;
        rec.setStatus(TaskState.COMPLETED.name());
        rec.setMessage(message);
        rec.setFinishedAt(LocalDateTime.now());
        if (rec.getStartedAt() != null) {
            rec.setDurationMs(java.time.Duration.between(rec.getStartedAt(), rec.getFinishedAt()).toMillis());
        }
        this.updateById(rec);
        return rec;
    }

    @Override
    public List<BuildRecordEntity> listUnfinishedRecords() {
        // 查询状态为 QUEUED 或 RUNNING 的记录
        return baseMapper.selectList(new LambdaQueryWrapper<BuildRecordEntity>()
                .in(BuildRecordEntity::getStatus , Arrays.asList(TaskState.QUEUED.name() , TaskState.RUNNING.name()))
                .orderByAsc(BuildRecordEntity::getEnqueuedAt) // 按入队时间排序，恢复原队列顺序
        );
    }
}
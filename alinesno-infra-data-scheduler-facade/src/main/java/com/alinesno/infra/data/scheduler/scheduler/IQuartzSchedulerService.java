package com.alinesno.infra.data.scheduler.scheduler;

import com.alinesno.infra.data.scheduler.enums.JobStatusEnums;
import lombok.SneakyThrows;

import java.io.IOException;
import java.sql.SQLException;

/**
 * 分布式调度任务服务接口
 * 用于创建、停止、移除和列举任务
 */
public interface IQuartzSchedulerService {

    /**
     * 执行定时任务
     * @param jobId
     */
    void createCronJob(Long jobId , long jobInstanceId) throws SQLException, IOException;

    /**
     * 添加任务
     * @param id
     * @param jobCron
     */
    void addJob(String id, String jobCron);

    /**
     * 更新任务
     * @param jobId
     * @param newCron
     */
    void updateJobCron(String jobId, String newCron);

    @SneakyThrows
    JobStatusEnums getJobStatus(String jobId);
}

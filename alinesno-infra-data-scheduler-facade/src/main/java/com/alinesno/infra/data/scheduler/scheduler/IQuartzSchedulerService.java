package com.alinesno.infra.data.scheduler.scheduler;

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

}

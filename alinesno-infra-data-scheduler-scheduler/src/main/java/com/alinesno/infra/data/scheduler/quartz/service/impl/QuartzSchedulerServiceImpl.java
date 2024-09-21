package com.alinesno.infra.data.scheduler.quartz.service.impl;

import com.alinesno.infra.data.scheduler.scheduler.IQuartzSchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Scheduler;
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

    @Override
    public void addJob(String id, String jobCron) {

    }
}

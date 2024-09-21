package com.alinesno.infra.data.scheduler.executor;

import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;

/**
 * 任务执行器
 */
public interface IExecutorService {

    /**
     * 执行任务，用于job执行实例
     * @param task
     */
    void execute(TaskInfoBean task) ;

}

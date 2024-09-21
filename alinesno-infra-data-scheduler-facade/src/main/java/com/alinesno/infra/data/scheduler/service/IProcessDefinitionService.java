package com.alinesno.infra.data.scheduler.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.TaskDefinitionEntity;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;

import java.util.List;

public interface IProcessDefinitionService extends IBaseService<ProcessDefinitionEntity> {

    /**
     * 运行任务实例
     * @param task
     * @param taskDefinitionList
     */
    void runProcess(TaskInfoBean task, List<TaskDefinitionEntity> taskDefinitionList);

}

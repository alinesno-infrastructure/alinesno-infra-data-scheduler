package com.alinesno.infra.data.scheduler.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.api.ProcessDefinitionDto;
import com.alinesno.infra.data.scheduler.api.ProcessTaskValidateDto;
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

    /**
     * 保存流程定义
     *
     * @param dto
     * @return
     */
    long commitProcessDefinition(ProcessDefinitionDto dto);

    /**
     * 运行验证任务
     * @param dto
     */
    void runProcessTask(ProcessTaskValidateDto dto);

    /**
     * 查询最近count条流程定义
     * @param count
     * @return
     */
    List<ProcessDefinitionEntity> queryRecentlyProcess(int count);

    /**
     * 更新流程定义
     * @param dto
     */
    void updateProcessDefinition(ProcessDefinitionDto dto);

}

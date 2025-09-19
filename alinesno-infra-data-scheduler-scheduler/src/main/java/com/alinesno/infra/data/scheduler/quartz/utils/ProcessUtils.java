package com.alinesno.infra.data.scheduler.quartz.utils;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.utils.DateUtils;
import com.alinesno.infra.data.scheduler.api.*;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.ProcessInstanceEntity;
import com.alinesno.infra.data.scheduler.entity.TaskDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.TaskInstanceEntity;
import com.alinesno.infra.data.scheduler.enums.ExecutorTypeEnums;
import com.alinesno.infra.data.scheduler.enums.ProcessStatusEnums;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProcessUtils {

    /**
     * 将流程任务转换为流程实例
     *
     * @param process
     * @param count
     * @return
     */
    public static ProcessInstanceEntity fromTaskToProcessInstance(ProcessDefinitionEntity process, long count) {

        ProcessInstanceEntity processInstance = new ProcessInstanceEntity();
        processInstance.setName(process.getName() + "#" + count);
        processInstance.setDescription(process.getDescription());
        processInstance.setProjectId(process.getProjectId());
        processInstance.setState(ProcessStatusEnums.RUNNING.getCode());
        processInstance.setRunTimes((int) count);
        processInstance.setHost("location");
        processInstance.setMaxTryTimes(0);
        processInstance.setTimeout(process.getTimeout());
        processInstance.setGlobalParams(process.getGlobalParams());
        processInstance.setRecovery(0);
        processInstance.setStartTime(new Date());

        // 权限权限
        processInstance.setOperatorId(process.getOperatorId());
        processInstance.setDepartmentId(process.getDepartmentId());
        processInstance.setOrgId(process.getOrgId());
        processInstance.setProcessId(process.getProjectId());

        return processInstance ;
    }

    /**
     * 将任务转换为任务实例
     *
     * @param process
     * @param t
     * @param id
     * @return
     */
    public static TaskInstanceEntity fromTaskToTaskInstance(ProcessDefinitionEntity process, TaskDefinitionEntity t, Long processInstanceId) {

        TaskInstanceEntity taskInstance = new TaskInstanceEntity();

        taskInstance.setName(t.getName());
        taskInstance.setTaskType(t.getTaskType());
        taskInstance.setState(ProcessStatusEnums.UNRULY.getCode());
        taskInstance.setProcessInstanceId(processInstanceId);
        taskInstance.setProcessId(process.getId());
        taskInstance.setDescription(t.getDescription());
        taskInstance.setRetryTimes(0);
        taskInstance.setMaxRetryTimes(t.getFailRetryTimes());
        taskInstance.setRetryInterval(t.getFailRetryInterval());
        taskInstance.setTaskParams(t.getTaskParams());
        taskInstance.setStartTime(new Date());

        return taskInstance ;
    }

    /**
     * 将流程定义转换为流程实体
     * @param dto
     * @return
     */
    public static ProcessDefinitionEntity fromDtoToEntity(ProcessDefinitionDto dto) {

        ProcessDefinitionEntity entity = new ProcessDefinitionEntity();
        ProcessContextDto context = dto.getContext();

        entity.setIcon(context.getIcon());
        entity.setName(context.getTaskName());
        entity.setProjectId(context.getProjectCode());
        entity.setGlobalParams(context.getGlobalParams()!= null? JSONObject.toJSONString(context.getGlobalParams()):null);
        entity.setTimeout(context.getTimeout());
        entity.setDataCollectionTemplate(context.getDataCollectionTemplate());
        entity.setScheduleCron(context.getCronExpression());
        entity.setMonitorEmail(context.getMonitorEmail());
        entity.setEnvId(context.getEnvId());
        entity.setCategoryId(context.getCategoryId());

        entity.setProjectId(dto.getProjectId());
        entity.setOrgId(dto.getOrgId());
        entity.setOperatorId(dto.getOperatorId());
        entity.setDepartmentId(dto.getDepartmentId());
        entity.setApplicationId(dto.getApplicationId());

        entity.setStartTime(DateUtils.parseDate(context.getStartTime()));
        entity.setEndTime(DateUtils.parseDate(context.getEndTime()));

        return entity ;

    }

    /**
     * 将流程定义转换为任务实体
     * @param dto
     * @param processId
     * @return
     */
    public static List<TaskDefinitionEntity> fromDtoToTaskInstance(ProcessDefinitionDto dto, long processId , long projectId) {

        List<TaskDefinitionEntity> taskDefinitionList = new ArrayList<>() ;

        List<ProcessTaskDto> taskFlow = dto.getTaskFlow();

        int orderNum = 1 ;
        for (ProcessTaskDto task : taskFlow) {

            TaskDefinitionEntity entity = new TaskDefinitionEntity();
            ParamsDto params = task.getParams();

            if(task.getTaskId() != 0){
                entity.setId(task.getTaskId());
            }

            String name = task.getName() ;
            String desc = task.getDescription() ;
            int retryCount = 0 ;
            String taskParams = null;
            String resourceId = null ;
            if(params != null){
                if(StringUtils.isNoneBlank(params.getName())){
                    name = params.getName() ;
                }
                if(StringUtils.isNoneBlank(params.getDesc())){
                    desc = params.getDesc() ;
                }
                retryCount = params.getRetryCount() ;
                taskParams = JSONObject.toJSONString(params) ;
                resourceId = JSONObject.toJSONString(params.getResourceId()) ;
            }

            entity.setCode(task.getId());
            entity.setProcessId(processId);
            entity.setName(name) ;
            entity.setProjectId(projectId);
            entity.setDescription(desc) ;
            entity.setTaskType(ExecutorTypeEnums.fromType(task.getType()).getCode());
            entity.setTaskParams(taskParams) ;
            entity.setFailRetryTimes(retryCount) ;
            entity.setResourceId(resourceId) ;
            entity.setOrderNum(orderNum);
            entity.setAttr(JSONObject.toJSONString(task.getAttr()));

            entity.setOrgId(dto.getOrgId());
            entity.setOperatorId(dto.getOperatorId());
            entity.setDepartmentId(dto.getDepartmentId());
            entity.setApplicationId(dto.getApplicationId());

            taskDefinitionList.add(entity);
            orderNum ++ ;
        }

        return taskDefinitionList ;
    }

    public static ProcessDefinitionEntity fromSaveDtoToEntity(ProcessDefinitionSaveDto dto) {
        ProcessDefinitionEntity entity = new ProcessDefinitionEntity();

        if(dto.getId() != null && dto.getId() > 0){
            entity.setId(dto.getId());
        }

        entity.setIcon(dto.getIcon());
        entity.setName(dto.getTaskName());
        entity.setDescription(dto.getTaskDesc());
        entity.setEnvId(dto.getEnvId());
        entity.setCategoryId(dto.getCategoryId());

        entity.setOrgId(dto.getOrgId());
        entity.setOperatorId(dto.getOperatorId());
        entity.setDepartmentId(dto.getDepartmentId());
        entity.setApplicationId(dto.getApplicationId());

        return entity ;
    }
}

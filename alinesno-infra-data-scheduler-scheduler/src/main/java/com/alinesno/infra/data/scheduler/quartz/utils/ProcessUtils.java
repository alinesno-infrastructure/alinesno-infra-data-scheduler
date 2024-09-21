package com.alinesno.infra.data.scheduler.quartz.utils;

import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.ProcessInstanceEntity;
import com.alinesno.infra.data.scheduler.entity.TaskDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.TaskInstanceEntity;
import com.alinesno.infra.data.scheduler.enums.ProcessStatusEnums;

import java.util.Date;

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
        processInstance.setProcessCode(process.getCode());
        processInstance.setName(process.getName() + "#" + count);
        processInstance.setDescription(process.getDescription());
        processInstance.setProjectCode(process.getProjectCode());
        processInstance.setState(ProcessStatusEnums.RUNNING.getCode());
        processInstance.setRunTimes((int) count);
        processInstance.setHost("location");
        processInstance.setMaxTryTimes(0);
        processInstance.setTimeout(process.getTimeout());
        processInstance.setGlobalParams(process.getGlobalParams());
        processInstance.setRecovery(0);
        processInstance.setStartTime(new Date());

        return processInstance ;
    }

    /**
     * 将任务转换为任务实例
     *
     * @param process
     * @param t
     * @return
     */
    public static TaskInstanceEntity fromTaskToTaskInstance(ProcessDefinitionEntity process, TaskDefinitionEntity t) {

        TaskInstanceEntity taskInstance = new TaskInstanceEntity();

        taskInstance.setName(t.getName());
        taskInstance.setTaskType(t.getTaskType());
        taskInstance.setState(ProcessStatusEnums.RUNNING.getCode());
        taskInstance.setProcessCode(process.getCode());
        taskInstance.setTaskCode(t.getCode());
        taskInstance.setDescription(t.getDescription());
        taskInstance.setRetryTimes(0);
        taskInstance.setMaxRetryTimes(t.getFailRetryTimes());
        taskInstance.setRetryInterval(t.getFailRetryInterval());
        taskInstance.setTaskParams(t.getTaskParams());
        taskInstance.setStartTime(new Date());

        return taskInstance ;
    }
}

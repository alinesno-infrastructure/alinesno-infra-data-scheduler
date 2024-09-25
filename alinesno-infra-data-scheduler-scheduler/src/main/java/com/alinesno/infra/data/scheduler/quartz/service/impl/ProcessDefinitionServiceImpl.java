package com.alinesno.infra.data.scheduler.quartz.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.common.web.log.utils.SpringUtils;
import com.alinesno.infra.data.scheduler.api.ProcessDefinitionDto;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.ProcessInstanceEntity;
import com.alinesno.infra.data.scheduler.entity.TaskDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.TaskInstanceEntity;
import com.alinesno.infra.data.scheduler.enums.ProcessStatusEnums;
import com.alinesno.infra.data.scheduler.executor.IExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.alinesno.infra.data.scheduler.quartz.mapper.ProcessDefinitionMapper;
import com.alinesno.infra.data.scheduler.quartz.utils.ProcessUtils;
import com.alinesno.infra.data.scheduler.scheduler.IQuartzSchedulerService;
import com.alinesno.infra.data.scheduler.service.IProcessDefinitionService;
import com.alinesno.infra.data.scheduler.service.IProcessInstanceService;
import com.alinesno.infra.data.scheduler.service.ITaskDefinitionService;
import com.alinesno.infra.data.scheduler.service.ITaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class ProcessDefinitionServiceImpl extends IBaseServiceImpl<ProcessDefinitionEntity , ProcessDefinitionMapper> implements IProcessDefinitionService {

    @Autowired
    private IQuartzSchedulerService distSchedulerService ;

    @Autowired
    private ITaskDefinitionService taskDefinitionService ;

    @Autowired
    private IProcessInstanceService processInstanceService ;

    @Autowired
    private ITaskInstanceService taskInstanceService ;

    @Async
    @Override
    public void runProcess(TaskInfoBean task, List<TaskDefinitionEntity> taskDefinition) {

        IExecutorService executorService  ;

        // 任务实例运行
        ProcessDefinitionEntity process = task.getProcess() ;

        // 记录任务流程实例开始
        long count = processInstanceService.count() + 1 ;
        ProcessInstanceEntity processInstance = ProcessUtils.fromTaskToProcessInstance(process , count) ;
        processInstanceService.save(processInstance) ;

        // 任务开始更新状态
        for(TaskDefinitionEntity t : taskDefinition){
            task.setTask(t);

            // 记录任务实例开始
            TaskInstanceEntity taskInstance = ProcessUtils.fromTaskToTaskInstance(process , t) ;

            String beanName = t.getTaskType() + "Executor" ;
            log.debug("TaskExecutor BeanName:{}" , beanName);

            executorService = SpringUtils.getBean(beanName) ;
            executorService.execute(task);

            // 更新任务实例结束
            taskInstance.setState(ProcessStatusEnums.END.getCode());
            taskInstance.setEndTime(new Date());
            taskInstanceService.update(taskInstance);
        }

        // 任务流程结束，更新任务实例
        processInstance.setState(ProcessStatusEnums.END.getCode());
        processInstance.setEndTime(new Date());
        processInstanceService.update(processInstance);
    }

    @Override
    public long saveProcessDefinition(ProcessDefinitionDto dto) {
        long projectId = dto.getProjectId() ;

        ProcessDefinitionEntity processDefinition = ProcessUtils.fromDtoToEntity(dto) ;
        this.save(processDefinition) ;

        long processId = processDefinition.getId() ;
        List<TaskDefinitionEntity> taskDefinitionList = ProcessUtils.fromDtoToTaskInstance(dto , processId , projectId) ;
        taskDefinitionService.saveBatch(taskDefinitionList) ;

        log.debug("saveProcessDefinition:{}" , processId);

        // 生成job任务
        distSchedulerService.addJob(processId + "" , dto.getContext().getCronExpression());

        return processId ;
    }

}

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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
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

    @Value("${alinesno.data.scheduler.workspacePath:#{systemProperties['java.io.tmpdir']}}")
    private String workspacePath;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)  // 此方法不需要开启事务
    @Override
    public void runProcess(TaskInfoBean task, List<TaskDefinitionEntity> taskDefinition) {

        // 任务实例运行
        ProcessDefinitionEntity process = task.getProcess() ;
        process.setRunCount(process.getRunCount()+1);
        updateById(process) ;

        // 执行运行实例及任务
        runProcessInstance(task, taskDefinition, process);

        // 更新流程运行成功次数
        process.setSuccessCount(process.getSuccessCount()+1);
        updateById(process);
    }

    /**
     * 运行流程实例
     * @param task
     * @param taskDefinition
     * @param process
     */
    @SneakyThrows
    private void runProcessInstance(TaskInfoBean task, List<TaskDefinitionEntity> taskDefinition, ProcessDefinitionEntity process) {
        IExecutorService executorService;

        // 记录任务流程实例开始
        long count = processInstanceService.count(new LambdaQueryWrapper<ProcessInstanceEntity>().eq(ProcessInstanceEntity::getProcessId , process.getId())) + 1 ;
        ProcessInstanceEntity processInstance = ProcessUtils.fromTaskToProcessInstance(process, count) ;
        processInstance.setProcessId(process.getId()) ;
        processInstance.setState(ProcessStatusEnums.RUNNING.getCode());

        // 生成实例工作空间
        String instanceWorkspace = process.getId() + File.separator + count ;
        FileUtils.forceMkdir(new File(workspacePath + File.separator + instanceWorkspace)); // 创建工作空间
        processInstance.setWorkspace(instanceWorkspace);

        processInstanceService.save(processInstance) ;
        task.setWorkspace(instanceWorkspace);

        boolean isFailTask = false ;

        // 任务开始更新状态
        for(TaskDefinitionEntity t : taskDefinition){
            task.setTask(t);

            // 记录任务实例开始
            TaskInstanceEntity taskInstance = ProcessUtils.fromTaskToTaskInstance(process, t , processInstance.getId()) ;
            taskInstance.setState(ProcessStatusEnums.RUNNING.getCode());
            taskInstance.setStartTime(new Date());
            taskInstanceService.save(taskInstance);

            String beanName = t.getTaskType() + "Executor" ;
            log.debug("TaskExecutor BeanName:{}" , beanName);

            executorService = SpringUtils.getBean(beanName) ;
            try{
                executorService.execute(task);

                // 更新任务实例结束
                taskInstance.setState(ProcessStatusEnums.END.getCode());
                taskInstance.setEndTime(new Date());
                taskInstanceService.update(taskInstance);

            }catch (Exception e){

                log.error("任务执行异常：" , e);

                taskInstance.setState(ProcessStatusEnums.FAIL.getCode());
                taskInstance.setEndTime(new Date());
                taskInstance.setErrorMsg(e.getMessage());
                taskInstanceService.update(taskInstance);

                isFailTask = true ;

                // 如果异常中断则直接跳出，否则继续执行
                if(!t.isContinueIgnore()){
                    break;
                }
            }
        }

        if(isFailTask){
            processInstance.setState(ProcessStatusEnums.FAIL.getCode());
        }else{
            // 任务流程结束，更新任务实例
            processInstance.setState(ProcessStatusEnums.END.getCode());
        }

        processInstance.setEndTime(new Date());
        processInstanceService.update(processInstance);
    }

    @Override
    public long commitProcessDefinition(ProcessDefinitionDto dto) {
        long projectId = dto.getProjectId() ;

        ProcessDefinitionEntity processDefinition = ProcessUtils.fromDtoToEntity(dto) ;
        processDefinition.setOnline(false);
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

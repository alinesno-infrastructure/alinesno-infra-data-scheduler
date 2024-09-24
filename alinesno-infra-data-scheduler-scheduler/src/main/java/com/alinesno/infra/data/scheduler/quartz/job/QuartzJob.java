package com.alinesno.infra.data.scheduler.quartz.job;

import com.alinesno.infra.data.scheduler.entity.EnvironmentEntity;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.ResourceEntity;
import com.alinesno.infra.data.scheduler.entity.TaskDefinitionEntity;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.alinesno.infra.data.scheduler.service.IEnvironmentService;
import com.alinesno.infra.data.scheduler.service.IProcessDefinitionService;
import com.alinesno.infra.data.scheduler.service.IResourceService;
import com.alinesno.infra.data.scheduler.service.ITaskDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.ArrayList;
import java.util.List;

// 禁止并发执行
@Slf4j
@DisallowConcurrentExecution
public class QuartzJob extends QuartzJobBean {

    @Autowired
    private IProcessDefinitionService processDefinitionService;

    @Autowired
    private ITaskDefinitionService taskDefinitionService;

    @Autowired
    private IEnvironmentService environmentService;

    @Autowired
    private IResourceService resourceService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        Long processId = jobDataMap.getLong("processId");

        List<String> resourceIds = new ArrayList<>();

        ProcessDefinitionEntity processDefinition = processDefinitionService.getById(processId);
        List<TaskDefinitionEntity> taskDefinitionList = taskDefinitionService.lambdaQuery().eq(TaskDefinitionEntity::getProcessId, processId).list();
        EnvironmentEntity environment = environmentService.lambdaQuery().eq(EnvironmentEntity::getProcessCode, processId).getEntity();

        taskDefinitionList.forEach(taskDefinition -> resourceIds.add(taskDefinition.getResourceId()));
        List<ResourceEntity> resourceList = resourceService.lambdaQuery().in(ResourceEntity::getId, resourceIds).list();

        // 运行任务实例
        TaskInfoBean task = new TaskInfoBean();
        task.setProcess(processDefinition);
        task.setEnvironment(environment);
        task.setResources(resourceList);

        processDefinitionService.runProcess(task, taskDefinitionList);
    }
}
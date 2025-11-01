package com.alinesno.infra.data.scheduler.trigger.service;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 流程执行服务
 */
@Slf4j
@Service
public class ProcessExecutionService {

    @Autowired
    private IProcessDefinitionService processDefinitionService;

    @Autowired
    private ITaskDefinitionService taskDefinitionService;

    @Autowired
    private IEnvironmentService environmentService;

    @Autowired
    private IResourceService resourceService;

    @Qualifier("threadPoolTaskExecutor")
    @Autowired
    private ThreadPoolTaskExecutor EXECUTOR ;

    /**
     * 执行流程（阻塞直到完成或被中断）
     */
    public void runProcess(Long processId) throws Exception {

        // 准备数据，跟 FlowQuartzJob.getResult 保持一致
        ProcessDefinitionEntity processDefinition = processDefinitionService.getById(processId);
        List<TaskDefinitionEntity> taskDefinitionList =
                taskDefinitionService.lambdaQuery().eq(TaskDefinitionEntity::getProcessId, processId).list();

        EnvironmentEntity environment = null;
        try {
            environment = environmentService.lambdaQuery().eq(EnvironmentEntity::getProcessId, processId).getEntity();
        } catch (Exception e) {
            // 可记录，但不一定中断
        }

        List<String> resourceIds = taskDefinitionList.stream()
                .map(TaskDefinitionEntity::getResourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<ResourceEntity> resourceList = resourceIds.isEmpty()
                ? Collections.emptyList()
                : resourceService.lambdaQuery().in(ResourceEntity::getId, resourceIds).list();

        TaskInfoBean taskInfo = new TaskInfoBean();
        taskInfo.setProcess(processDefinition);
        taskInfo.setEnvironment(environment);
        taskInfo.setResources(resourceList);

        CompletableFuture<String>  future = processDefinitionService.runProcess(taskInfo, taskDefinitionList);
        future.get();

    }
}
package com.alinesno.infra.data.scheduler.adapter;

import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.enums.ExecutionStrategyEnums;
import com.dtflys.forest.annotation.BaseRequest;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 任务执行工作流
 */
@BaseRequest(baseURL = "#{alinesno.infra.gateway.host}/data-scheduler" , connectTimeout = 30*1000)
public interface WorkerFlowConsumer {

    /**
     * 运行Agent角色
     *
     */
    String runRoleFlow(Long processDefinitionId ,
                       ProcessDefinitionEntity processDefinitionEntity ,
                       ExecutionStrategyEnums errorStrategy ,
                       Map<String , String> orgSecrets);

    /**
     * 运行Agent角色
     *
     */
    CompletableFuture<String> runRoleFlow(Long processDefinitionId) ;

}

package com.alinesno.infra.data.scheduler.workflow.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.workflow.dto.FlowDto;
import com.alinesno.infra.data.scheduler.workflow.dto.WorkflowRequestDto;
import com.alinesno.infra.data.scheduler.workflow.entity.FlowEntity;

import java.util.concurrent.CompletableFuture;

/**
 * 工作流服务接口，负责处理工作流基础信息和元数据信息相关的业务操作。
 * 继承自 IBaseService 接口，借助其提供的通用方法，可对工作流数据进行基本的增删改查等操作。
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
public interface IFlowService extends IBaseService<FlowEntity> {

    /**
     * 保存角色工作流信息
     * @param processDefinitionId
     * @param flowDto
     */
    void saveRoleFlow(Long processDefinitionId, WorkflowRequestDto flowDto);

    /**
     * 发布工作流
     * @param processDefinitionId
     */
    void publishFlow(Long processDefinitionId);

    /**
     * 获取指定角色最新版本的已发布流程
     * @param processDefinitionId 角色ID
     * @return 最新版本的已发布流程实体，如果不存在则返回 null
     */
    FlowEntity getLatestPublishedFlowByProcessDefinitionId(Long processDefinitionId);

    /**
     * 获取指定角色的未发布流程
     * @param processDefinitionId 角色ID
     * @return 未发布流程实体，如果不存在则返回 null
     */
    FlowEntity getUnpublishedFlowByProcessDefinitionId(Long processDefinitionId);

    /**
     * 获取指定角色最新版本的已发布流程
     * @param processDefinitionId 角色ID
     * @return 最新版本的已发布流程实体，如果不存在则返回 null
     */
    FlowDto getLatestFlowByProcessDefinitionId(Long processDefinitionId);

    /**
     * 运行Agent角色
     *
     */
    CompletableFuture<String> runRoleFlow(Long processDefinitionId);

    /**
     * 尝试运行工作流
     *
     * @param processDefinitionId
     * @return
     */
    CompletableFuture<String> tryRun(Long processDefinitionId);
}
package com.alinesno.infra.data.scheduler.adapter.worker;

import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.api.worker.FlowDto;
import com.alinesno.infra.data.scheduler.api.worker.LastExecuteFlowDto;
import com.alinesno.infra.data.scheduler.api.worker.RunRoleFlowDto;
import com.alinesno.infra.data.scheduler.api.worker.WorkflowRequestDto;
import com.alinesno.infra.data.scheduler.entity.worker.FlowEntity;
import com.dtflys.forest.annotation.*;

/**
 * 任务执行工作流
 */
@BaseRequest(baseURL = "#{alinesno.data.scheduler.worker-node}/api/infra/data/scheduler/flow" , readTimeout = 3600_000)
public interface WorkerFlowConsumer {

    /**
     * 运行Agent角色
     *
     */
    @Post("/runRoleFlow")
    R<String> runRoleFlow(@JSONBody RunRoleFlowDto runRoleFlowDto);

    /**
     * 发布角色
     * @param processDefinitionId
     */
    @Post("/publish")
    R<Boolean> publishFlow(@Query("processDefinitionId") Long processDefinitionId);

    /**
     * 获取最新流程
     * @param processDefinitionId
     * @return
     */
    @Get("/latest")
    R<FlowDto> getLatestFlowByProcessDefinitionId(@Query("processDefinitionId") Long processDefinitionId);

    /**
     * 获取最新已发布流程
     * @param processDefinitionId
     * @return
     */
    @Get("/latestPublished")
    R<FlowEntity> getLatestPublishedFlowByProcessDefinitionId(@Query("processDefinitionId") Long processDefinitionId);

    /**
     * 获取最近执行流程
     * @param processDefinitionId
     * @param executeId
     * @return
     */
    @Get("/executedFlowNodes")
    R<LastExecuteFlowDto> getLastExecutedFlow(@Query("processDefinitionId") Long processDefinitionId,
                                              @Query("executeId") Long executeId);

    /**
     * 获取未发布流程
     * @param processDefinitionId
     * @return
     */
    @Get("/unpublished")
    R<FlowEntity> getUnpublishedFlowByProcessDefinitionId(@Query("processDefinitionId") Long processDefinitionId);

    /**
     * 保存流程
     * @param flowDto
     * @param processDefinitionId
     * @return
     */
    @Post("/processAndSave")
    R<Long> processAndSave(@JSONBody WorkflowRequestDto flowDto, @Query("processDefinitionId") Long processDefinitionId);

    /**
     * 获取最近执行流程
     * @param processDefinitionId
     * @return
     */
    @Get("/lastExecutedFlowNodes")
    R<LastExecuteFlowDto> lastExecutedFlowNodes(@Query("processDefinitionId") Long processDefinitionId);
}

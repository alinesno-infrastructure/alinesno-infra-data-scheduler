package com.alinesno.infra.data.scheduler.workflow.dto;

import lombok.Data;

import java.util.List;

/**
 * 最近执行流程数据传输对象
 * 用于封装流程执行的最新状态和节点执行信息
 */
@Data
public class LastExecuteFlowDto {

    /**
     * 流程执行状态
     * 表示整个流程的执行结果状态
     */
    private String status ;

    /**
     * 流程节点执行信息列表
     * 包含流程中各个节点的执行详情
     */
    private List<FlowNodeExecutionDto> flowNode ;

}

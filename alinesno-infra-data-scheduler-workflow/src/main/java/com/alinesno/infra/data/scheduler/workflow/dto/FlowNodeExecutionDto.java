package com.alinesno.infra.data.scheduler.workflow.dto;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.facade.base.BaseDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 流程节点执行DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FlowNodeExecutionDto extends BaseDto {

    /**
     * 所属工作流执行实例的 ID
     */
    private Long flowExecutionId;

    /**
     * 节点 ID
     */
    private Long nodeId;

    /**
     * 界面工作流步骤节点ID
     */
    private String stepId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 执行信息
     */
    private String executeInfo ;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 执行进度按最高100进行表示，0-100之间
     */
    private Integer progress;

    /**
     * 节点的属性，采用键值对的形式存储额外信息
     * 这里假设以 JSON 字符串形式存储，实际使用时可根据需求调整
     */
    private String properties;

    /**
     * 执行状态
     */
    private String executionStatus;

    /**
     * 执行的序号
     */
    private Integer executionOrder;

    /**
     * 执行的图形深度
     */
    private Integer executionDepth;

    /**
     * 开始时间
     */
    private Date executeTime;

    /**
     * 结束时间
     */
    private Date finishTime;

    /**
     * 错误信息
     */
    private String errorMsg ;

    /**
     * 节点数据信息
     */
    private JSONObject node;

}

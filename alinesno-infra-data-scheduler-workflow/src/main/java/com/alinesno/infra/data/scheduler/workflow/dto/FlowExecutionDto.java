package com.alinesno.infra.data.scheduler.workflow.dto;

import com.alinesno.infra.common.facade.base.BaseDto;
import com.baomidou.mybatisplus.annotation.TableField;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnComment;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnType;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 工作流执行数据传输对象
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FlowExecutionDto extends BaseDto {

    /**
     * 工作流名称
     */
    private String name ;

    /**
     * 所属工作流的 ID
     */
    private Long flowId;

    /**
     * 流程定义ID
     */
    private Long processDefinitionId ;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 工作流图JSON数据
     */
    private String flowGraphJson; // 工作流图JSON数据

    /**
     * 执行状态
     */
    private String executionStatus;

    /**
     * 开始时间
     */
    private Date executeTime;

    /**
     * 结束时间
     */
    private Date finishTime;

    /**
     * 运行次数
     */
    private Long runTimes;

    /**
     * 运行唯一号
     */
    private String runUniqueNumber;

    /**
     * 执行状态标签
     */
    private String executionStatusLabel ;

}

package com.alinesno.infra.data.scheduler.api;

import com.alinesno.infra.common.facade.base.BaseDto;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ProcessDefinitionSaveDto 类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProcessDefinitionSaveDto extends BaseDto {

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId ;

    /**
     * 流程描述
     */
    @NotNull(message = "流程描述不能为空")
    private String taskDesc ;

    /**
     * 环境ID
     */
    @NotNull(message = "环境ID不能为空")
    private Long envId ;

    /**
     * 图标
     */
    @NotNull(message = "图标不能为空")
    private String icon ;

    /**
     * 任务名称
     */
    @NotNull(message = "任务名称不能为空")
    private String taskName ;

    /**
     * 异常策略
     */
    @NotNull(message = "异常策略不能为空")
    private Integer errorStrategy ;

}

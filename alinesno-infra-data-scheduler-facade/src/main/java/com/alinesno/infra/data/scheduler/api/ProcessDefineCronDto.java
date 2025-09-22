package com.alinesno.infra.data.scheduler.api;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * ProcessDefineCronDto
 */
@Data
public class ProcessDefineCronDto {

    /**
     * cron表达式
     */
    @NotNull(message = "cron不能为空")
    private String cron ;

    /**
     * 流程定义ID
     */
    @NotNull(message = "processDefineId不能为空")
    private Long processDefineId;

}

package com.alinesno.infra.data.scheduler.trigger.bean;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * JobCreateDto
 */
@Data
public class JobCreateDto {

    private Long id ;

    @NotNull(message = "processId不能为空")
    private Long processId ;

    @NotNull(message = "name不能为空")
    private String name ;

    @NotNull(message = "备注不能为空")
    private String remark ;

    @NotNull(message = "cron不能为空")
    private String cron ;

}

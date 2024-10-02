package com.alinesno.infra.data.scheduler.api;

import com.alinesno.infra.common.facade.base.BaseDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class EnvironmentDto extends BaseDto {

    @NotBlank(message = "系统环境不能为空")
    private String systemEnv ;

    @NotBlank(message = "名称不能为空")
    private String name;

    @NotBlank(message = "配置不能为空")
    private String config;

    @NotBlank(message = "描述不能为空")
    private String description;

    @NotBlank(message = "范围不能为空")
    private String credentialScope ;
}

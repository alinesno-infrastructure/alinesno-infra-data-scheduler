package com.alinesno.infra.data.scheduler.api;

import com.alinesno.infra.common.facade.base.BaseDto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 保存项目
 */
@ToString
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectDto extends BaseDto {

    /**
     * 项目名称
     */
    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    /**
     * 项目描述
     */
    @NotBlank(message = "项目描述不能为空")
    private String projectDesc;

}

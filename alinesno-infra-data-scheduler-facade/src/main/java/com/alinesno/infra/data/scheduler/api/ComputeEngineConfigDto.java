package com.alinesno.infra.data.scheduler.api;


import com.alinesno.infra.common.facade.base.BaseDto;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

/**
 * 计算引擎配置
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ComputeEngineConfigDto extends BaseDto {

    @NotBlank(message = "计算引擎不能为空")
    @Pattern(regexp = "^(spark|flink|trino)$", message = "计算引擎必须为 spark|flink|trino")
    private String computeEngine;

    @NotBlank(message = "引擎地址不能为空")
    @Size(min = 3, max = 1024, message = "引擎地址长度 3-1024")
    private String engineAddress;

    @Size(max = 1024, message = "运行的管理员用户")
    private String apiToken;

    @Min(value = 1, message = "请求超时时间最小为 1 秒")
    @Max(value = 300, message = "请求超时时间最大为 300 秒")
    private Integer requestTimeout;

    /**
     * 从实体转换为 DTO
     * @param computeEngineEntity
     * @return
     */
    public static ComputeEngineConfigDto fromEntity(ComputeEngineEntity computeEngineEntity) {
        ComputeEngineConfigDto dto = new ComputeEngineConfigDto();
        BeanUtils.copyProperties(computeEngineEntity, dto);
        return dto;
    }

    /**
     * 从 DTO 转换为实体
     * @param config
     * @return
     */
    public static ComputeEngineEntity toEntity(ComputeEngineConfigDto config) {
        ComputeEngineEntity entity = new ComputeEngineEntity();
        BeanUtils.copyProperties(config, entity);
        return entity;
    }
}
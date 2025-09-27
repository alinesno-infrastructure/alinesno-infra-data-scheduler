package com.alinesno.infra.data.scheduler.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProbeResultDto
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProbeResultDto {
    /**
     * 探针结果
     */
    private boolean success;

    /**
     * 探针结果信息
     */
    private String message;

    /**
     * HTTP 状态码
     */
    private int httpStatus; // 可选
}
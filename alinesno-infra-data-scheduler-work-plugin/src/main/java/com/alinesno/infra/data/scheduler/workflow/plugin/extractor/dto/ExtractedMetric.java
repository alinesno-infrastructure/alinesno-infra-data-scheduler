// ExtractedMetric.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 指标抽取结果
 * <p>
 * 表示从文档中成功提取的一个结构化指标值，
 * 包含置信度、来源位置等审计信息。
 * </p>
 */
@Data
public class ExtractedMetric {
    /** 指标名称 */
    private String indicatorName;

    /** 提取的数值 */
    private BigDecimal value;

    /** 单位 */
    private String unit;

    /** 年份 */
    private Integer year;

    /** 来源文件名 */
    private String sourceFile;

    /** 位置标识，如 "P12", "Sheet1!B5" */
    private String location;

    /** 置信度 [0.0, 1.0] */
    private double confidence;

    /** 是否通过校验规则 */
    private boolean verified;
}
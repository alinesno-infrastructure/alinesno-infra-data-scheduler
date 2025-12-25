// IndicatorDefinition.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto;

import lombok.Data;
import java.util.List;

/**
 * 指标定义数据传输对象（DTO）
 * <p>
 * 用于动态描述一个待提取的业务指标，如“净利润”、“资产负债率”等。
 * 所有字段均可在运行时由上游系统传入，无需预注册。
 * </p>
 */
@Data
public class IndicatorDefinition {
    /** 指标名称，如 "净利润" */
    private String name;

    /** 指标别名列表，用于关键词匹配，如 ["net profit", "归母净利润"] */
    private List<String> aliases;

    /** 预期单位，如 "亿元"、"%"。若为空则不校验单位 */
    private String unit;

    /** 目标年份（可选） */
    private Integer year;

    /** 值类型，如 "NUMBER", "PERCENTAGE", "TEXT" */
    private String valueType;

    /** 数值最小值（用于校验） */
    private Double minValue;

    /** 数值最大值（用于校验） */
    private Double maxValue;

    /** 允许的单位列表（优先级高于 unit 字段） */
    private List<String> allowedUnits;
}
// LLMStructuredExtraction.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * LLM 结构化抽取的标准输出格式（通用、非行业限定）
 * <p>
 * 用于强制大模型返回可解析的 JSON，字段设计为通用语义：
 * - 支持任意指标名（indicator_name）
 * - 值可以是数值（value）或文本（text_value）
 * - 包含来源位置与置信度
 * </p>
 */
@Data
public class LLMStructuredExtraction {

    /**
     * 抽取的指标列表
     */
    @JsonProperty("extracted_fields")
    private List<ExtractedField> extractedFields;

    /**
     * 单个抽取字段
     */
    @Data
    public static class ExtractedField {
        /**
         * 指标名称（与用户输入的 indicator.name 一致）
         */
        @JsonProperty("field_name")
        private String fieldName;

        /**
         * 数值型结果（如 2023, 150.5）
         * 若为文本型，此字段为 null
         */
        @JsonProperty("numeric_value")
        private BigDecimal numericValue;

        /**
         * 文本型结果（如 "阿里巴巴集团"）
         * 若为数值型，此字段可为空
         */
        @JsonProperty("text_value")
        private String textValue;

        /**
         * 单位（如 "人", "%", "万元"）
         */
        @JsonProperty("unit")
        private String unit;

        /**
         * 来源位置描述（如 "第3页第2段", "表格A1-B5"）
         */
        @JsonProperty("source_location")
        private String sourceLocation;

        /**
         * 置信度 [0.0, 1.0]，由 LLM 自评或系统设定
         */
        @JsonProperty("confidence")
        private Double confidence;
    }
}
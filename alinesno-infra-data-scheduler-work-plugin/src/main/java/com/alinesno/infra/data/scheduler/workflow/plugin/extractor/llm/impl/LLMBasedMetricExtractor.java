// LLMBasedMetricExtractor.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.llm.impl;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.context.ExtractionContext;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.*;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.engine.MetricExtractor;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.llm.GenericLLMClient;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.llm.LLMPromptBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于 LLM 的通用结构化抽取引擎（兜底策略）
 * <p>
 * 当规则引擎无法处理时，调用 LLM 进行智能抽取。
 * 不限定行业，完全依赖用户传入的指标定义。
 * </p>
 */
@Component
@Order(2) // 优先级低于规则引擎
public class LLMBasedMetricExtractor implements MetricExtractor {

    @Autowired
    private GenericLLMClient llmClient;

    @Override
    public ExtractionResult extract(ExtractionContext ctx) {
        IndicatorDefinition indicator = ctx.getIndicator();
        List<DocumentChunk> chunks = ctx.getRelevantChunks();

        // 合并所有 chunk 的文本（简化处理，也可逐 chunk 调用）
        StringBuilder fullText = new StringBuilder();
        for (DocumentChunk chunk : chunks) {
            fullText.append("[来源: ").append(chunk.getSourceFile())
                    .append(" 位置: ");
            if (chunk.getPageNumber() != null) {
                fullText.append("P").append(chunk.getPageNumber());
            } else if (chunk.getSectionPath() != null) {
                fullText.append(chunk.getSectionPath());
            } else {
                fullText.append("N/A");
            }
            fullText.append("]\n")
                    .append(chunk.getContent())
                    .append("\n\n");
        }

        try {
            // 构建提示（仅当前指标）
            String prompt = LLMPromptBuilder.buildExtractionPrompt(
                    List.of(indicator),
                    fullText.toString()
            );

            // 调用 LLM
            LLMStructuredExtraction output = llmClient.extractStructured(prompt);

            // 转换为 ExtractedMetric
            List<ExtractedMetric> metrics = new ArrayList<>();
            if (output.getExtractedFields() != null) {
                for (LLMStructuredExtraction.ExtractedField field : output.getExtractedFields()) {
                    if (!indicator.getName().equals(field.getFieldName())) continue;

                    ExtractedMetric metric = new ExtractedMetric();
                    metric.setIndicatorName(field.getFieldName());
                    metric.setValue(field.getNumericValue());
                    metric.setUnit(field.getUnit());
                    metric.setSourceFile(ctx.getRelevantChunks().get(0).getSourceFile()); // 简化
                    metric.setLocation(field.getSourceLocation());
                    metric.setConfidence(field.getConfidence() != null ? field.getConfidence() : 0.8);
                    metric.setVerified(false); // LLM 结果需后续验证

                    metrics.add(metric);
                }
            }

            return new ExtractionResult(metrics, List.of("Used LLM fallback"));

        } catch (Exception e) {
            return new ExtractionResult(
                    List.of(),
                    List.of("LLM extraction failed: " + e.getMessage())
            );
        }
    }

    @Override
    public String getStrategyName() {
        return "LLM_STRUCTURED";
    }
}
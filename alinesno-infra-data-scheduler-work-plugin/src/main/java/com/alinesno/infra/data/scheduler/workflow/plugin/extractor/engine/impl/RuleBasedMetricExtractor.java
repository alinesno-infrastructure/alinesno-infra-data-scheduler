// RuleBasedMetricExtractor.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.engine.impl;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.context.ExtractionContext;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.DocumentChunk;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.ExtractedMetric;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.ExtractionResult;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.IndicatorDefinition;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.engine.MetricExtractor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于规则的指标抽取引擎（线程安全）
 * <p>
 * 使用正则表达式匹配指标名称+数值，优先处理高精度场景。
 * 无内部状态，可安全并发调用。
 * </p>
 */
@Component
@Order(1)
public class RuleBasedMetricExtractor implements MetricExtractor {

    @Override
    public ExtractionResult extract(ExtractionContext ctx) {
        IndicatorDefinition indicator = ctx.getIndicator();
        List<DocumentChunk> chunks = ctx.getRelevantChunks();

        List<ExtractedMetric> metrics = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 动态构建关键词正则（转义特殊字符）
        List<String> allKeywords = new ArrayList<>();
        allKeywords.add(indicator.getName());
        if (indicator.getAliases() != null) {
            allKeywords.addAll(indicator.getAliases());
        }
        String keywordPattern = String.join("|",
            allKeywords.stream().map(Pattern::quote).collect(Collectors.toList())
        );

        // 构造数值匹配正则
        String unitPattern = indicator.getUnit() != null ? Pattern.quote(indicator.getUnit()) : "[亿万千百%‰]?[元美元]?";
        Pattern valuePattern = Pattern.compile(
            "(" + keywordPattern + ")" +
            "[\\s:：]*" +
            "([\\d,]+\\.?\\d*)" +
            "\\s*(" + unitPattern + ")?",
            Pattern.CASE_INSENSITIVE
        );

        for (DocumentChunk chunk : chunks) {
            Matcher m = valuePattern.matcher(chunk.getContent());
            while (m.find()) {
                try {
                    String numStr = m.group(2).replace(",", "");
                    BigDecimal value = new BigDecimal(numStr);
                    String unit = m.group(3);

                    if (!isValidValue(indicator, value)) {
                        warnings.add("数值 " + value + " 超出合理范围");
                        continue;
                    }
                    if (!isValidUnit(indicator, unit)) {
                        warnings.add("单位 " + unit + " 不符合预期");
                        continue;
                    }

                    ExtractedMetric metric = new ExtractedMetric();
                    metric.setIndicatorName(indicator.getName());
                    metric.setValue(value);
                    metric.setUnit(unit);
                    metric.setYear(indicator.getYear());
                    metric.setSourceFile(chunk.getSourceFile());
                    metric.setLocation(buildLocation(chunk));
                    metric.setConfidence(0.95);
                    metric.setVerified(true);

                    metrics.add(metric);
                } catch (Exception e) {
                    warnings.add("解析数值失败: " + m.group(2));
                }
            }
        }

        return new ExtractionResult(metrics, warnings);
    }

    private boolean isValidValue(IndicatorDefinition ind, BigDecimal value) {
        if (ind.getMinValue() != null && value.compareTo(BigDecimal.valueOf(ind.getMinValue())) < 0) {
            return false;
        }
        if (ind.getMaxValue() != null && value.compareTo(BigDecimal.valueOf(ind.getMaxValue())) > 0) {
            return false;
        }
        return true;
    }

    private boolean isValidUnit(IndicatorDefinition ind, String unit) {
        if (unit == null) unit = "";
        if (ind.getAllowedUnits() != null && !ind.getAllowedUnits().isEmpty()) {
            return ind.getAllowedUnits().contains(unit);
        }
        if (ind.getUnit() != null) {
            return ind.getUnit().equals(unit);
        }
        return true;
    }

    private String buildLocation(DocumentChunk chunk) {
        if (chunk.getPageNumber() != null) {
            return "P" + chunk.getPageNumber();
        }
        if (chunk.getSectionPath() != null && !chunk.getSectionPath().isEmpty()) {
            return chunk.getSectionPath();
        }
        return "N/A";
    }

    @Override
    public String getStrategyName() {
        return "RULE_BASED_DYNAMIC";
    }
}
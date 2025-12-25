package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier.impl;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.ExtractedMetric;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.IndicatorDefinition;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier.MetricVerifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// UnitNormalizationVerifier.java
@Component
@Order(2)
public class UnitNormalizationVerifier implements MetricVerifier {

    // 单位换算表（可配置化）
    private static final Map<String, UnitFactor> UNIT_MAP = Map.ofEntries(
        Map.entry("万人", new UnitFactor("人", 10_000)),
        Map.entry("亿元", new UnitFactor("元", 100_000_000)),
        Map.entry("万", new UnitFactor("", 10_000)),
        Map.entry("亿", new UnitFactor("", 100_000_000)),
        Map.entry("%", new UnitFactor("%", 1))
    );

    @Override
    public List<ExtractedMetric> verify(IndicatorDefinition indicator, List<ExtractedMetric> metrics) {
        return metrics.stream().map(m -> {
            if (m.getValue() == null || m.getUnit() == null) return m;

            UnitFactor factor = UNIT_MAP.get(m.getUnit());
            if (factor != null) {
                ExtractedMetric normalized = new ExtractedMetric();
                normalized.setIndicatorName(m.getIndicatorName());
                normalized.setValue(m.getValue().multiply(BigDecimal.valueOf(factor.multiplier)));
                normalized.setUnit(factor.targetUnit);
                normalized.setSourceFile(m.getSourceFile());
                normalized.setLocation(m.getLocation());
                normalized.setConfidence(m.getConfidence());
                normalized.setVerified(true); // 标记已标准化
                return normalized;
            }
            return m;
        }).toList();
    }

    @Override public String getName() { return "UNIT_NORMALIZATION"; }

    private static class UnitFactor {
        String targetUnit;
        double multiplier;
        UnitFactor(String target, double mult) {
            this.targetUnit = target;
            this.multiplier = mult;
        }
    }
}
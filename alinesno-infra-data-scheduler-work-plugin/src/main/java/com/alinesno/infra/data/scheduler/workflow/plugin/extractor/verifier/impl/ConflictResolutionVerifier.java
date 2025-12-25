package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier.impl;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.ExtractedMetric;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.IndicatorDefinition;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier.MetricVerifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

// ConflictResolutionVerifier.java
@Component
@Order(4)
public class ConflictResolutionVerifier implements MetricVerifier {

    @Override
    public List<ExtractedMetric> verify(IndicatorDefinition indicator, List<ExtractedMetric> metrics) {
        if (metrics.size() <= 1) return metrics;

        // 按置信度降序，取 top 1
        return metrics.stream()
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .limit(1)
                .toList();
    }

    @Override public String getName() { return "CONFLICT_RESOLUTION"; }
}
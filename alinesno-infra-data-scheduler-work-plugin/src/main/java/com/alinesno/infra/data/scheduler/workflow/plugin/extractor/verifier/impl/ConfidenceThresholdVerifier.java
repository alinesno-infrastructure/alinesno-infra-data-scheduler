package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier.impl;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.ExtractedMetric;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.IndicatorDefinition;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier.MetricVerifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

// ConfidenceThresholdVerifier.java
@Component
@Order(5)
public class ConfidenceThresholdVerifier implements MetricVerifier {

    private static final double CONFIDENCE_THRESHOLD = 0.7;

    @Override
    public List<ExtractedMetric> verify(IndicatorDefinition indicator, List<ExtractedMetric> metrics) {
        // 此验证器不修改列表，仅用于分离待复核项
        // 实际分离逻辑在 VerificationService 中处理
        return metrics;
    }

    public boolean needsReview(ExtractedMetric metric) {
        return metric.getConfidence() < CONFIDENCE_THRESHOLD;
    }

    @Override public String getName() { return "CONFIDENCE_CHECK"; }
}
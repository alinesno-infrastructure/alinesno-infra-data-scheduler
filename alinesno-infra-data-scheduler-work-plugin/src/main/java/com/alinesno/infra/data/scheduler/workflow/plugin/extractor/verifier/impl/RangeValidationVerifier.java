package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier.impl;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.ExtractedMetric;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.IndicatorDefinition;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier.MetricVerifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

// RangeValidationVerifier.java
@Component
@Order(3)
public class RangeValidationVerifier implements MetricVerifier {

    @Override
    public List<ExtractedMetric> verify(IndicatorDefinition indicator, List<ExtractedMetric> metrics) {
        BigDecimal min = BigDecimal.valueOf(indicator.getMinValue());
        BigDecimal max = BigDecimal.valueOf(indicator.getMaxValue());

        return metrics.stream().filter(m -> {
            if (m.getValue() == null) return false;
            if (m.getValue().compareTo(min) < 0) return false;
            return m.getValue().compareTo(max) <= 0;
        }).peek(m -> m.setVerified(true)).toList();
    }

    @Override public String getName() { return "RANGE_VALIDATION"; }
}
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier.impl;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.ExtractedMetric;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.IndicatorDefinition;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier.MetricVerifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

// DuplicateMergerVerifier.java
@Component
@Order(1)
public class DuplicateMergerVerifier implements MetricVerifier {

    @Override
    public List<ExtractedMetric> verify(IndicatorDefinition indicator, List<ExtractedMetric> metrics) {
        return metrics.stream()
                .collect(Collectors.toMap(
                        m -> m.getValue() + "|" + m.getUnit() + "|" + m.getSourceFile(),
                        m -> m,
                        (m1, m2) -> {
                            // 保留更高置信度的
                            if (m2.getConfidence() > m1.getConfidence()) return m2;
                            else return m1;
                        },
                        LinkedHashMap::new
                ))
                .values().stream().toList();
    }

    @Override public String getName() { return "DUPLICATE_MERGER"; }
}
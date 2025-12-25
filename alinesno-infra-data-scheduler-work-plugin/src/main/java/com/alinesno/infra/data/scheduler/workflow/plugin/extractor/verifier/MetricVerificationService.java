package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.ExtractedMetric;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.VerificationContext;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.VerificationResult;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier.impl.ConfidenceThresholdVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// MetricVerificationService.java
@Service
public class MetricVerificationService {

    @Autowired
    private List<MetricVerifier> verifiers; // Spring 自动注入所有实现

    @Autowired
    private ConfidenceThresholdVerifier confidenceVerifier;

    public VerificationResult verify(VerificationContext ctx) {
        List<String> logs = new ArrayList<>();
        List<ExtractedMetric> current = new ArrayList<>(ctx.getRawMetrics());

        logs.add("原始指标数量: " + current.size());

        // 依次应用验证器
        for (MetricVerifier verifier : verifiers) {
            current = verifier.verify(ctx.getIndicator(), current);
            logs.add(verifier.getName() + " → 剩余: " + current.size());
        }

        // 分离待复核项
        List<ExtractedMetric> verified = new ArrayList<>();
        List<ExtractedMetric> pending = new ArrayList<>();

        for (ExtractedMetric m : current) {
            if (confidenceVerifier.needsReview(m)) {
                pending.add(m);
            } else {
                verified.add(m);
            }
        }

        return new VerificationResult(verified, pending, logs);
    }
}
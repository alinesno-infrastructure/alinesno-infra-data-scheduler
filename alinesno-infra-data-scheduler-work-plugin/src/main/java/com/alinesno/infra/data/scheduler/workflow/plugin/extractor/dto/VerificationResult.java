// VerificationResult.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 验证后的最终结果
 */
@NoArgsConstructor
@Data
public class VerificationResult {
    private List<ExtractedMetric> verifiedMetrics; // 通过验证的指标
    private List<ExtractedMetric> pendingReviewMetrics; // 需人工复核
    private List<String> validationLogs; // 审计日志（用于调试/追踪）

    public VerificationResult(List<ExtractedMetric> verified, List<ExtractedMetric> pending, List<String> logs) {
        this.verifiedMetrics = verified;
        this.pendingReviewMetrics = pending;
        this.validationLogs = logs;
    }
}
// VerificationContext.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 验证任务的输入上下文
 */
@NoArgsConstructor
@Data
public class VerificationContext {
    private IndicatorDefinition indicator;
    private List<IndicatorDefinition> indicators;
    private List<ExtractedMetric> rawMetrics; // 原始抽取结果（可能来自多策略）

    public VerificationContext(List<IndicatorDefinition> indicators, List<ExtractedMetric> allMetrics) {
        this.indicators = indicators;
        this.rawMetrics = allMetrics;
    }
}
// MetricVerifier.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.ExtractedMetric;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.IndicatorDefinition;

import java.util.List;

/**
 * 指标验证器接口（策略模式）
 */
public interface MetricVerifier {
    /**
     * 对原始指标列表进行验证/修正
     * @param indicator 指标定义
     * @param metrics 原始抽取结果
     * @return 修正后的指标列表
     */
    List<ExtractedMetric> verify(IndicatorDefinition indicator, List<ExtractedMetric> metrics);
    
    String getName();
}
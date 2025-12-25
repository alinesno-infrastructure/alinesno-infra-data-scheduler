// BatchExtractionResult.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量指标抽取结果
 * <p>
 * 聚合所有指标的抽取结果，按指标-文档维度展开。
 * </p>
 */
@NoArgsConstructor
@Data
public class BatchExtractionResult {
    /** 所有成功提取的指标值列表 */
    private List<ExtractedMetric> metrics;

    public BatchExtractionResult(List<ExtractedMetric> metrics) {
        this.metrics = metrics;
    }
}
// BatchMetricExtractionService.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.service;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.BatchExtractionRequest;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.BatchExtractionResult;

/**
 * 批量指标抽取服务接口
 */
public interface BatchMetricExtractionService {
    BatchExtractionResult extractBatch(BatchExtractionRequest request);
}
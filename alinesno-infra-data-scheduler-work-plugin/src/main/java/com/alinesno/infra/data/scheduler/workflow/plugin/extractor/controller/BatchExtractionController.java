// BatchExtractionController.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.controller;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.BatchExtractionRequest;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.BatchExtractionResult;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.service.BatchMetricExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 批量指标抽取 REST API
 */
@RestController
@RequestMapping("/api/v1/metrics")
public class BatchExtractionController {

    @Autowired
    private BatchMetricExtractionService extractionService;

    @PostMapping("/extract/batch")
    public BatchExtractionResult extractBatch(@RequestBody BatchExtractionRequest request) {
        return extractionService.extractBatch(request);
    }
}
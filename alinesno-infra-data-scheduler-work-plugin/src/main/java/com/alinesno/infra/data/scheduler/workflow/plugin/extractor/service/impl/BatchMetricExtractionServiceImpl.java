// BatchMetricExtractionServiceImpl.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.service.impl;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.context.ExtractionContext;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.*;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.engine.MetricExtractor;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.service.BatchMetricExtractionService;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.storage.DocumentChunkRepository;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.storage.MetadataIndex;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.verifier.MetricVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 批量指标抽取服务实现（线程安全）
 * <p>
 * 核心流程：
 * 1. 合并所有指标关键词，一次性检索相关文档块
 * 2. 按文件分组，每个文件独立并行处理
 * 3. 对每个文件，尝试所有抽取引擎（规则优先）
 * </p>
 */
@Service
public class BatchMetricExtractionServiceImpl implements BatchMetricExtractionService {

    @Autowired
    private MetadataIndex metadataIndex;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private MetricVerificationService metricVerificationService ;

    @Autowired
    private List<MetricExtractor> extractors; // Spring 自动注入所有实现

    // 自定义线程池，避免阻塞主线程
    private final ExecutorService extractionExecutor = Executors.newFixedThreadPool(
        Math.min(16, Runtime.getRuntime().availableProcessors() * 2)
    );

    @Override
    public BatchExtractionResult extractBatch(BatchExtractionRequest request) {
        List<IndicatorDefinition> indicators = request.getIndicators();
        if (indicators == null || indicators.isEmpty()) {
            return new BatchExtractionResult(Collections.emptyList());
        }

        // Step 1: 收集所有唯一关键词
        Set<String> allKeywords = new HashSet<>();
        for (IndicatorDefinition ind : indicators) {
            allKeywords.add(ind.getName());
            if (ind.getAliases() != null) {
                allKeywords.addAll(ind.getAliases());
            }
        }

        // Step 2: 全局初筛（用所有关键词检索）
        List<DocumentChunk> relevantStubs = metadataIndex.searchBulk(
            new ArrayList<>(allKeywords),
            Set.of("PDF", "DOCX", "XLSX", "PPTX"),
            ".*",
            request.getSourceFiles()
        );

        // Step 3: 加载完整文档块（去重）
        Set<String> uniqueFiles = relevantStubs.stream()
            .map(DocumentChunk::getSourceFile)
            .collect(Collectors.toSet());
        List<DocumentChunk> allRelevantChunks = new ArrayList<>();
        for (String file : uniqueFiles) {
            allRelevantChunks.addAll(chunkRepository.findBySource(file));
        }

        // Step 4: 按文件分组
        Map<String, List<DocumentChunk>> chunksByFile = allRelevantChunks.stream()
            .collect(Collectors.groupingBy(DocumentChunk::getSourceFile));

        // Step 5: 并行处理每个文件
        List<CompletableFuture<List<ExtractedMetric>>> futures = new ArrayList<>();
        for (Map.Entry<String, List<DocumentChunk>> entry : chunksByFile.entrySet()) {
            String file = entry.getKey();
            List<DocumentChunk> chunks = entry.getValue();

            CompletableFuture<List<ExtractedMetric>> future = CompletableFuture.supplyAsync(() -> {
                return extractMetricsForFile(chunks, indicators);
            }, extractionExecutor);

            futures.add(future);
        }

        // Step 6: 聚合结果
        List<ExtractedMetric> allMetrics = futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        VerificationResult verificationResult = metricVerificationService.verify(
                new VerificationContext(indicators, allMetrics)
        );

        return new BatchExtractionResult(allMetrics);
    }

    private List<ExtractedMetric> extractMetricsForFile(List<DocumentChunk> chunks, List<IndicatorDefinition> indicators) {
        List<ExtractedMetric> fileMetrics = new ArrayList<>();

        for (IndicatorDefinition indicator : indicators) {
            // 为当前指标筛选相关 chunks（简单包含判断）
            List<DocumentChunk> candidateChunks = chunks.stream()
                .filter(chunk -> {
                    String content = chunk.getContent().toLowerCase();
                    return content.contains(indicator.getName().toLowerCase()) ||
                           (indicator.getAliases() != null && indicator.getAliases().stream()
                               .anyMatch(alias -> content.contains(alias.toLowerCase())));
                })
                .collect(Collectors.toList());

            if (candidateChunks.isEmpty()) continue;

            ExtractionContext ctx = new ExtractionContext();
            ctx.setIndicator(indicator);
            ctx.setRelevantChunks(candidateChunks);

            // 按优先级尝试抽取引擎
            for (MetricExtractor extractor : extractors) {
                ExtractionResult result = extractor.extract(ctx);
                if (result.getMetrics() != null &&
                    result.getMetrics().stream().allMatch(m -> m.getConfidence() >= 0.9)) {
                    fileMetrics.addAll(result.getMetrics());
                    break; // 高置信度，不再尝试其他引擎
                }
            }
        }

        return fileMetrics;
    }
}
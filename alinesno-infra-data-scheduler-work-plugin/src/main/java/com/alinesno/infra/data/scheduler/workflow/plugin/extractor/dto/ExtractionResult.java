// ExtractionResult.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto;

import lombok.Data;
import java.util.Collections;
import java.util.List;

/**
 * 单次指标抽取的结果封装
 * <p>
 * 由 {@link com.alinesno.infra.data.scheduler.workflow.engine.MetricExtractor} 返回，
 * 包含成功提取的指标值及过程警告信息。
 * 为保证线程安全，内部列表采用防御性拷贝（defensive copy）策略。
 * </p>
 */
@Data
public class ExtractionResult {

    /**
     * 成功提取的指标值列表
     * <p>
     * 可能为空（未找到），也可能包含多个（如跨年份、多来源）
     * </p>
     */
    private List<ExtractedMetric> metrics;

    /**
     * 抽取过程中产生的警告或调试信息（如单位不匹配、数值越界等）
     * <p>
     * 用于日志记录或人工复核，不影响主流程。
     * </p>
     */
    private List<String> warnings;

    /**
     * 构造函数（推荐使用）
     * <p>
     * 自动进行防御性拷贝，防止外部修改内部状态。
     * </p>
     * @param metrics 提取的指标列表（可为 null）
     * @param warnings 警告信息列表（可为 null）
     */
    public ExtractionResult(List<ExtractedMetric> metrics, List<String> warnings) {
        this.metrics = (metrics == null) ? Collections.emptyList() : List.copyOf(metrics);
        this.warnings = (warnings == null) ? Collections.emptyList() : List.copyOf(warnings);
    }

    /**
     * 默认构造函数（Lombok 生成，但建议优先使用带参构造）
     */
    public ExtractionResult() {
        this.metrics = Collections.emptyList();
        this.warnings = Collections.emptyList();
    }

    /**
     * 判断本次抽取是否产生有效结果
     * @return true 如果至少有一个指标被提取
     */
    public boolean hasMetrics() {
        return this.metrics != null && !this.metrics.isEmpty();
    }
}
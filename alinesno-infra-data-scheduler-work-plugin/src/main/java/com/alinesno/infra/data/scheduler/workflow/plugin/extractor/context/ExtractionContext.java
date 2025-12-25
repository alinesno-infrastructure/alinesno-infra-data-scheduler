// ExtractionContext.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.context;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.DocumentChunk;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.IndicatorDefinition;
import lombok.Data;
import java.util.List;

/**
 * 指标抽取执行上下文
 * <p>
 * 封装单次指标抽取任务所需的全部输入信息。
 * 该对象由服务层构建，并传递给具体的 {@link com.alinesno.infra.data.scheduler.workflow.engine.MetricExtractor} 实现。
 * 设计为不可变（immutable）风格以支持并发安全：所有字段在构造后不应被修改。
 * </p>
 */
@Data
public class ExtractionContext {

    /**
     * 当前待抽取的指标定义
     */
    private IndicatorDefinition indicator;

    /**
     * 与该指标相关的文档块列表（已通过索引初筛）
     * <p>
     * 注意：此列表应仅包含可能包含目标指标的文本块，
     * 避免让抽取引擎遍历全量文档。
     * </p>
     */
    private List<DocumentChunk> relevantChunks;

    /**
     * 构造函数（可选，Lombok 已生成 setter/getter）
     * 若需强一致性，可移除 @Data 并手动实现 final 字段 + 构造器
     */
    public ExtractionContext() {}

    /**
     * 辅助方法：判断上下文是否有效
     * @return true 如果 indicator 和 relevantChunks 均非空
     */
    public boolean isValid() {
        return this.indicator != null && this.relevantChunks != null && !this.relevantChunks.isEmpty();
    }
}
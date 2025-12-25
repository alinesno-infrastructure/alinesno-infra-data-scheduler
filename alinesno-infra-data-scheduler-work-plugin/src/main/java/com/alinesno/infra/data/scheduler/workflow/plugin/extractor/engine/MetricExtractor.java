// MetricExtractor.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.engine;


import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.context.ExtractionContext;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.ExtractionResult;

/**
 * 指标抽取引擎接口
 * <p>
 * 定义通用的指标抽取契约。实现类应是无状态的（线程安全），
 * 以便被多线程共享调用。
 * </p>
 */
public interface MetricExtractor {
    /**
     * 执行指标抽取
     * @param context 抽取上下文
     * @return 抽取结果
     */
    ExtractionResult extract(ExtractionContext context);

    /**
     * 获取策略名称，用于日志或调试
     * @return 策略名，如 "RULE_BASED"
     */
    String getStrategyName();
}
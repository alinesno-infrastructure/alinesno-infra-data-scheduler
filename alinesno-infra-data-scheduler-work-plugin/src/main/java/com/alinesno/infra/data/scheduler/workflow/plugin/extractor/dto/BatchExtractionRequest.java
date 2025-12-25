// BatchExtractionRequest.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto;

import lombok.Data;
import java.util.List;

/**
 * 批量指标抽取请求
 * <p>
 * 允许一次性传入多个指标定义，并可选限定文档范围，
 * 用于高效处理大规模指标抽取任务。
 * </p>
 */
@Data
public class BatchExtractionRequest {
    /** 待抽取的指标定义列表（支持数百个） */
    private List<IndicatorDefinition> indicators;

    /** 可选：限定处理的文件列表。若为空则处理所有已解析文档 */
    private List<String> sourceFiles;
}
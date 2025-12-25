// GenericLLMClient.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.llm;


import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.LLMStructuredExtraction;

/**
 * 通用 LLM 客户端接口（支持结构化输出）
 */
public interface GenericLLMClient {
    /**
     * 调用 LLM 并返回结构化解析结果
     * @param prompt 提示词（应包含格式约束）
     * @return 解析后的结构化对象
     * @throws Exception 网络错误、解析失败等
     */
    LLMStructuredExtraction extractStructured(String prompt) throws Exception;
}
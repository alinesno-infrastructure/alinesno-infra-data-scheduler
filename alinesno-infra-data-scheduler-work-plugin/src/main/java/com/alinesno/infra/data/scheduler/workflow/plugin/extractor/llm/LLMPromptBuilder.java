// LLMPromptBuilder.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.llm;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.IndicatorDefinition;

import java.util.List;

/**
 * 构建用于 LLM 结构化抽取的强约束提示词
 * <p>
 * 不依赖行业知识，仅基于用户提供的指标定义动态生成提示。
 * 强制模型输出纯 JSON，无前缀、无后缀、无解释。
 * </p>
 */
public class LLMPromptBuilder {

    /**
     * 构建结构化抽取提示
     *
     * @param indicators 用户定义的指标列表
     * @param documentText 待分析的文档文本
     * @return 可直接发送给 LLM 的提示字符串
     */
    public static String buildExtractionPrompt(
            List<IndicatorDefinition> indicators,
            String documentText) {

        // 动态生成字段描述
        StringBuilder fieldsDesc = new StringBuilder();
        for (IndicatorDefinition ind : indicators) {
            fieldsDesc.append("- 字段名: \"").append(ind.getName()).append("\"\n");
            if (ind.getAliases() != null && !ind.getAliases().isEmpty()) {
                fieldsDesc.append("  别名: ").append(String.join(", ", ind.getAliases())).append("\n");
            }
            if (ind.getValueType() != null) {
                fieldsDesc.append("  类型: ").append(ind.getValueType()).append("\n");
            }
            if (ind.getAllowedUnits() != null && !ind.getAllowedUnits().isEmpty()) {
                fieldsDesc.append("  允许单位: ").append(String.join(", ", ind.getAllowedUnits())).append("\n");
            }
            fieldsDesc.append("\n");
        }

        return """
        你是一个信息抽取专家。请从以下文本中提取指定字段的信息，并严格按照 JSON 格式输出。

        ### 提取规则：
        1. 仅输出 JSON 对象，不要包含任何其他文字、解释、Markdown 或代码块。
        2. 如果某个字段在文本中未出现，请不要包含该字段。
        3. 数值字段必须使用 numeric_value，文本字段使用 text_value。
        4. 所有字段名必须与下方“字段定义”中的 field_name 完全一致。
        5. source_location 应尽可能精确（如“第2页第1段”、“表格第3行”）。

        ### 字段定义：
        %s

        ### 待处理文本：
        %s

        ### 请输出 JSON（仅 JSON）：
        """.formatted(fieldsDesc.toString().trim(), documentText);
    }
}
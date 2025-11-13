// AiChatNode.java
package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.agentsflex.core.llm.Llm;
import com.agentsflex.core.llm.LlmConfig;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.data.scheduler.adapter.service.ILLmAdapterService;
import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.llm.entity.LlmModelEntity;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.AiChatNodeData;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * AI 对话节点，继承自 AbstractFlowNode 类。
 * 使用父类提供的 nodeLogService 记录日志。
 */
@Slf4j
@Data
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "ai_chat")
@EqualsAndHashCode(callSuper = true)
public class AiChatNode extends AbstractFlowNode {

    @Autowired
    private ILLmAdapterService adapterService;

    public AiChatNode() {
        setType("ai_chat");
    }

    @SneakyThrows
    @Override
    protected CompletableFuture<Void> handleNode() {
        // 开始执行日志（使用父类注入的 nodeLogService）
        try {
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "AI对话节点开始执行",
                    Map.of(
                            "nodeType", "ai_chat",
                            "flowExecutionId", flowExecution.getId(),
                            "runUniqueNumber", flowExecution.getRunUniqueNumber()
                    )
            ));
        } catch (Exception ignore) {
            log.warn("记录 AI 节点开始日志失败: {}", ignore.getMessage());
        }

        AiChatNodeData nodeData = getAiChatProperties();
        if (nodeData == null) {
            log.warn("AI 节点配置为空，跳过执行");
            try {
                nodeLogService.append(NodeLog.of(
                        flowExecution.getId().toString(),
                        node.getId(),
                        node.getStepName(),
                        "WARN",
                        "AI节点配置为空，跳过执行",
                        Map.of("phase", "config.empty")
                ));
            } catch (Exception ignore) {
                log.warn("记录 config.empty 日志失败: {}", ignore.getMessage());
            }
            return CompletableFuture.completedFuture(null);
        }

        String llmModelId = nodeData.getLlmModelId();
        LlmModelEntity llmModel = llmModelService.getById(llmModelId);
        if (llmModel == null || !"qwen".equals(llmModel.getProviderCode())) {
            String errorMsg = "LLM 模型不可用: " + llmModelId;
            eventMessage(errorMsg);
            try {
                nodeLogService.append(NodeLog.of(
                        flowExecution.getId().toString(),
                        node.getId(),
                        node.getStepName(),
                        "ERROR",
                        errorMsg,
                        Map.of(
                                "phase", "model.check",
                                "llmModelId", llmModelId,
                                "providerCode", llmModel != null ? llmModel.getProviderCode() : "null"
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录 model.check 日志失败: {}", ignore.getMessage());
            }
            return CompletableFuture.completedFuture(null);
        }

        // 构建 LLM 客户端并记录（脱敏示例）
        try {
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "构建LLM客户端",
                    Map.of(
                            "phase", "client.build",
                            "modelName", llmModel.getModel(),
                            "provider", llmModel.getProviderCode(),
                            "endpoint", llmModel.getApiUrl()
                    )
            ));
        } catch (Exception ignore) {
            log.warn("记录构建LLM客户端日志失败: {}", ignore.getMessage());
        }

        LlmConfig config = new LlmConfig();
        config.setEndpoint(llmModel.getApiUrl());
        config.setApiKey(llmModel.getApiKey());
        config.setModel(llmModel.getModel());
        Llm llm = adapterService.getLlm(llmModel.getProviderCode(), config);

        String prompt = replacePlaceholders(nodeData.getPrompt());
        try {
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "生成最终Prompt",
                    Map.of(
                            "phase", "prompt.process",
                            "prompt", prompt,
                            "promptLength", prompt.length(),
                            "promptPreview", prompt.length() > 200 ? prompt.substring(0, 200) + "..." : prompt
                    )
            ));
        } catch (Exception ignore) {
            log.warn("记录 prompt.process 日志失败: {}", ignore.getMessage());
        }

        outputContent.append("对话生成中，指令:").append(prompt);

        return getAiChatResultAsync(llm, prompt)
                .orTimeout(120, TimeUnit.SECONDS)
                .thenAcceptAsync(chatResult -> {
                    try {
                        nodeLogService.append(NodeLog.of(
                                flowExecution.getId().toString(),
                                node.getId(),
                                node.getStepName(),
                                "INFO",
                                "AI对话结果处理完成",
                                Map.of(
                                        "phase", "result.handle",
                                        "result", chatResult,
                                        "outputKey", node.getStepName() + ".answer",
                                        "resultLength", chatResult.length()
                                )
                        ));
                    } catch (Exception ignore) {
                        log.warn("记录 result.handle 日志失败: {}", ignore.getMessage());
                    }

                    output.put(node.getStepName() + ".answer", chatResult);
                    output.put(node.getStepName() + ".reasoning_content", chatResult);
                    outputContent.append("AI对话内容").append(chatResult);

                    if (node.isPrint()) {
                        try {
                            eventMessageCallbackMessage(chatResult);
                        } catch (Exception e) {
                            log.warn("追加输出内容失败:{}", e.getMessage());
                        }
                    }

                    try {
                        nodeLogService.append(NodeLog.of(
                                flowExecution.getId().toString(),
                                node.getId(),
                                node.getStepName(),
                                "INFO",
                                "AI对话节点执行成功",
                                Map.of(
                                        "phase", "node.complete",
                                        "totalPromptLength", prompt.length(),
                                        "totalResultLength", chatResult == null ? 0 : chatResult.length()
                                )
                        ));
                    } catch (Exception ignore) {
                        log.warn("记录 node.complete 日志失败: {}", ignore.getMessage());
                    }
                }, chatThreadPool)
                .exceptionally(ex -> {
                    String errorMsg = ex.getMessage() != null ? ex.getMessage() : "未知异常";
                    try {
                        nodeLogService.append(NodeLog.of(
                                flowExecution.getId().toString(),
                                node.getId(),
                                node.getStepName(),
                                "ERROR",
                                "AI对话节点执行失败: " + errorMsg,
                                Map.of(
                                        "phase", "node.exception",
                                        "exception", errorMsg,
                                        "stackTrace", StackTraceUtils.getStackTrace(ex).length() > 5000 ?
                                                StackTraceUtils.getStackTrace(ex).substring(0, 5000) + "..." :
                                                StackTraceUtils.getStackTrace(ex)
                                )
                        ));
                    } catch (Exception ignore) {
                        log.warn("记录 node.exception 日志失败: {}", ignore.getMessage());
                    }

                    log.error("LLM 调用失败:{}", ex.getMessage(), ex);
                    output.put(node.getStepName() + ".answer", "LLM 请求失败: " + ex.getMessage());
                    eventMessage("LLM 请求失败: " + ex.getMessage());
                    return null;
                });
    }

    private AiChatNodeData getAiChatProperties() {
        String nodeDataJson = node.getProperties().get("node_data") + "";
        return StringUtils.isNotEmpty(nodeDataJson) ?
                JSONObject.parseObject(nodeDataJson, AiChatNodeData.class) : null;
    }
}
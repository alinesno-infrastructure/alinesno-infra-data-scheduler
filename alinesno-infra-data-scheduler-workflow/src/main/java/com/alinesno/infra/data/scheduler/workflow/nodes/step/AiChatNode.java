// AiChatNode.java
package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.agentsflex.core.llm.Llm;
import com.agentsflex.core.llm.LlmConfig;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.data.scheduler.adapter.service.ILLmAdapterService;
import com.alinesno.infra.data.scheduler.llm.entity.LlmModelEntity;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.AiChatNodeData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 该类表示 AI 对话节点，继承自 AbstractFlowNode 类。
 * 用于在工作流中实现与 AI 大模型进行对话的功能。
 * 当工作流执行到该节点时，会触发与 AI 大模型的交互过程。
 */
@Slf4j
@Data
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "ai_chat")
@EqualsAndHashCode(callSuper = true)
public class AiChatNode extends AbstractFlowNode {

    @Autowired
    private ILLmAdapterService adapterService;

    /**
     * 构造函数，初始化节点类型为 "ai_chat"。
     */
    public AiChatNode() {
        setType("ai_chat");
    }

    @SneakyThrows
    @Override
    protected CompletableFuture<Void> handleNode() {
        AiChatNodeData nodeData = getAiChatProperties();
        if (nodeData == null) {
            log.warn("AI 节点配置为空，跳过执行");
            return CompletableFuture.completedFuture(null); // 空任务直接完成
        }

        // 1. 校验 LLM 模型配置（同步逻辑）
        String llmModelId = nodeData.getLlmModelId();
        LlmModelEntity llmModel = llmModelService.getById(llmModelId);
        if (llmModel == null || !"qwen".equals(llmModel.getProviderCode())) {
            eventMessage("LLM 模型不可用: " + llmModelId);
            return CompletableFuture.completedFuture(null);
        }

        // 2. 构建 LLM 客户端（同步逻辑，轻量级操作）
        LlmConfig config = new LlmConfig() ;

        config.setEndpoint(llmModel.getApiUrl());
        config.setApiKey(llmModel.getApiKey()) ;
        config.setModel(llmModel.getModel()) ;

        Llm llm = adapterService.getLlm(llmModel.getProviderCode(), config);

        // 3. 替换占位符（同步逻辑）
        String prompt = replacePlaceholders(nodeData.getPrompt());

        outputContent.append("对话生成中，指令:").append(prompt);

        // 4. 异步调用 LLM 接口（核心异步逻辑，使用 llmExecutor）
        return getAiChatResultAsync(llm, prompt)
                .orTimeout(120, TimeUnit.SECONDS) // 设置超时（必加！避免永久阻塞）
                .thenAcceptAsync(chatResult -> {
                    // 5. 处理 LLM 结果（异步回调，使用 orchestratorExecutor 处理短任务）
                    output.put(node.getStepName() + ".answer", chatResult);
                    output.put(node.getStepName() + ".reasoning_content", chatResult);

                    // AI对话内容
                    outputContent.append("AI对话内容").append(chatResult);

                    // 打印内容到输出（若配置）
                    if (node.isPrint()) {
                        try {
                            eventMessageCallbackMessage(chatResult); // 追加到 outputContent
                        } catch (Exception e) {
                            log.warn("追加输出内容失败:{}", e.getMessage());
                        }
                    }
                }, chatThreadPool)
                .exceptionally(ex -> {
                    // 6. 异常处理（异步回调）
                    log.error("LLM 调用失败:{}", ex.getMessage(), ex);
                    output.put(node.getStepName() + ".answer", "LLM 请求失败: " + ex.getMessage());
                    eventMessage("LLM 请求失败: " + ex.getMessage());
                    return null;
                });
    }

    private AiChatNodeData getAiChatProperties(){
        String nodeDataJson =  node.getProperties().get("node_data")+"" ;
        return StringUtils.isNotEmpty(nodeDataJson)? JSONObject.parseObject(nodeDataJson , AiChatNodeData.class):null;
    }

}
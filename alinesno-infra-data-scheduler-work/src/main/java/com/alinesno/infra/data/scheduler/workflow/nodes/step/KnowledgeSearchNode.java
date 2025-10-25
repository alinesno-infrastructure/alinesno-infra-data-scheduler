package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alinesno.infra.data.scheduler.workflow.constants.AgentConstants;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 该类表示知识库检索节点，继承自 AbstractFlowNode 类。
 * 主要用于关联知识库，查找与用户提出的问题相关的分段内容。
 * 在工作流中，当需要从知识库中获取相关信息时，会使用该节点。
 */
@Slf4j
@Data
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "knowledge_search")
@EqualsAndHashCode(callSuper = true)
public class KnowledgeSearchNode extends AbstractFlowNode {

    /**
     * 构造函数，初始化节点类型为 "knowledge_search"。
     */
    public KnowledgeSearchNode() {
        setType("knowledge_search");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {
        try {
            eventStepMessage("开始节点检索知识库", AgentConstants.STEP_START);

            String query = getQuery();
            String datasetIds = getDatasetIds();

            log.debug("KnowledgeSearchNode query = {}, datasetIds = {}", query, datasetIds);


            eventStepMessage("节点检索知识库", AgentConstants.STEP_FINISH);

            // 将检索结果写入输出上下文，保留原有几个键以兼容下游处理
//            output.put(node.getStepName() + ".paragraph_list", content);
//            output.put(node.getStepName() + ".is_hit_handling_method_list", content);
//            output.put(node.getStepName() + ".data", content);
//            output.put(node.getStepName() + ".directly_return", content);

            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            log.error("KnowledgeSearchNode 执行异常: {}", ex.getMessage(), ex);
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;
        }
    }

    /**
     * 从节点属性中获取检索使用的 dataset ids（逗号分隔），若不存在则返回空字符串。
     */
    private String getDatasetIds() {
        try {
            Object v = node.getProperties().get("dataset_ids");
            String ids = v == null ? "" : String.valueOf(v);
            log.debug("getDatasetIds = {}", ids);
            return ids;
        } catch (Exception ex) {
            log.warn("getDatasetIds 解析异常，返回空: {}", ex.getMessage());
            return "";
        }
    }

    /**
     * 从节点属性中获取检索使用的 query（优先级：node 属性中的 query -> node 名称 -> 默认值）。
     */
    private String getQuery() {
        try {
            Object v = node.getProperties().get("query");
            if (v != null && String.valueOf(v).trim().length() > 0) {
                return String.valueOf(v);
            }
            // 如果没有在属性中指定 query，尝试使用节点 stepName 或回退到默认值
            String stepName = node.getStepName();
            if (stepName != null && !stepName.trim().isEmpty()) {
                return stepName;
            }
        } catch (Exception ex) {
            log.warn("getQuery 解析异常，使用默认值: {}", ex.getMessage());
        }
        return "Java测试技术";
    }
}
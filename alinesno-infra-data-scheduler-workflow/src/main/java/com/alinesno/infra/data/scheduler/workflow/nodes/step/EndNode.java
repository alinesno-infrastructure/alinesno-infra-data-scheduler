package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.EndNodeData;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 结束节点
 */
@Slf4j
@Data
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "end")
@EqualsAndHashCode(callSuper = true)
public class EndNode extends AbstractFlowNode {

    public EndNode() {
        setType("end");
    }

    /**
     * 轻量同步处理并返回已完成的 CompletableFuture。
     * 该方法会在上层提交的线程（如 chatThreadPool）中执行，不会额外创建线程。
     */
    @Override
    protected CompletableFuture<Void> handleNode() {
        String taskId = String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown");
        String stepId = node != null ? node.getId() : "unknown";
        String stepName = node != null ? node.getStepName() : "unknown";

        // 开始日志（容错）
        try {
            nodeLogService.append(NodeLog.of(
                    taskId,
                    stepId,
                    stepName,
                    "INFO",
                    "End 节点开始执行",
                    Map.of(
                            "nodeType", "end"
                    )
            ));
        } catch (Exception ignore) {
            log.warn("记录 EndNode 开始日志失败: {}", ignore.getMessage());
        }

        try {
            EndNodeData nodeData = getNodeData();

            String answer = nodeData == null ? "" : nodeData.getReplayContent();
            answer = replacePlaceholders(answer);
            eventNodeMessage(answer);

            // 设置参数到 output（与原逻辑一致）
            output.put(node.getStepName() + ".output", answer);

            outputContent.append("节点运行结束:").append(answer);

            // 结束成功日志（容错）
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "INFO",
                        "End 节点执行完成",
                        Map.of(
                                "outputPreview", answer == null ? "" : (answer.length() > 500 ? answer.substring(0, 500) + "..." : answer),
                                "outputLength", answer == null ? 0 : answer.length()
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录 EndNode 完成日志失败: {}", ignore.getMessage());
            }

            // 轻量操作：直接返回已完成的 Future
            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            log.error("EndNode 执行异常: {}", ex.getMessage(), ex);

            // 错误日志（容错）
            try {
                String stack = StackTraceUtils.getStackTrace(ex);
                if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "ERROR",
                        "End 节点执行异常: " + ex.getMessage(),
                        Map.of(
                                "exception", ex.getMessage(),
                                "stackTrace", stack
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录 EndNode 异常日志失败: {}", ignore.getMessage());
            }

            // 将错误信息写入 output 以便后续处理
            output.put(node.getStepName() + ".output", "处理异常: " + ex.getMessage());
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;
        }
    }

    private EndNodeData getNodeData() {
        String nodeDataJson = String.valueOf(node.getProperties().get("node_data"));
        return JSONObject.parseObject(nodeDataJson, EndNodeData.class);
    }
}
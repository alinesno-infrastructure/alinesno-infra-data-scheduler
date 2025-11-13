package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 开始节点类（异步链签名），增加了详细节点日志
 */
@Slf4j
@Data
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "start")
@EqualsAndHashCode(callSuper = true)
public class StartNode extends AbstractFlowNode {

    public StartNode() {
        setType("start");
    }

    /**
     * 轻量同步处理并返回已完成的 CompletableFuture。
     * 该方法会在上层提交的线程（如 chatThreadPool）中执行，不会额外创建线程。
     */
    @Override
    protected CompletableFuture<Void> handleNode() {
        long startTs = System.currentTimeMillis();
        try {
            // 配置/启动日志
            nodeLogService.append(NodeLog.of(
                    flowExecution != null ? flowExecution.getId().toString() : "unknown",
                    node != null ? node.getId() : "unknown",
                    node != null ? node.getStepName() : "start",
                    "INFO",
                    "Start 节点开始执行",
                    Map.of(
                            "nodeType", "start",
                            "printEnable", node != null && node.isPrint()
                    )
            ));

            outputContent.append("任务开始");
            outputContent.append("\n");

            // 可选：把一些环境信息写到 output（示例）
            output.put(node.getStepName() + ".startedAt", System.currentTimeMillis());

            long durationMs = System.currentTimeMillis() - startTs;

            // 完成日志
            nodeLogService.append(NodeLog.of(
                    flowExecution != null ? flowExecution.getId().toString() : "unknown",
                    node.getId() ,
                    node.getStepName() ,
                    "INFO",
                    "Start 节点执行完成",
                    Map.of(
                            "durationMs", durationMs,
                            "outputKey", node.getStepName() + ".startedAt"
                    )
            ));

            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            String errMsg = ex.getMessage() != null ? ex.getMessage() : "未知异常";
            String stack = StackTraceUtils.getStackTrace(ex);
            if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";

            // 记录错误日志
            nodeLogService.append(NodeLog.of(
                    flowExecution != null ? flowExecution.getId().toString() : "unknown",
                    node != null ? node.getId() : "unknown",
                    node != null ? node.getStepName() : "start",
                    "ERROR",
                    "Start 节点执行异常: " + errMsg,
                    Map.of(
                            "exception", errMsg,
                            "stackTrace", stack
                    )
            ));

            log.error("StartNode 执行异常: {}", ex.getMessage(), ex);
            // 将错误信息写入 output 以便后续处理
            output.put(node.getStepName() + ".message", "处理异常: " + errMsg);

            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;
        }
    }
}
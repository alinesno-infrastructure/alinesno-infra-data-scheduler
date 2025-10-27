package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.ShellNodeData;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Shell 执行节点（增加了详细日志）
 */
@Slf4j
@Data
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "shell")
@EqualsAndHashCode(callSuper = true)
public class ShellNode extends AbstractFlowNode {

    public ShellNode() {
        setType("shell");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {
        long startTs = System.currentTimeMillis();
        try {
            // 解析节点配置
            ShellNodeData nodeData = getNodeData();
            log.debug("ShellNode nodeData = {}", nodeData);

            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "Shell节点配置解析完成",
                    Map.of(
                            "scriptLength", nodeData.getShellScript() != null ? nodeData.getShellScript().length() : 0,
                            "script" , nodeData.getShellScript() == null ? "" : nodeData.getShellScript(),
                            "environment", nodeData.getEnvironment(),
                            "printEnable", node.isPrint()
                    )
            ));

            String rawScript = nodeData.getShellScript();
            log.debug("Shell Executor rawScript: {}", rawScript);

            // 记录执行开始
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "Shell脚本开始执行",
                    Map.of(
                            "commandPreview", rawScript
                    )
            ));

            // 构建命令（保留多行支持）
            String command = """
                %s
                """.formatted(rawScript);

            String executeResult = runCommand(command);

            long durationMs = System.currentTimeMillis() - startTs;
            int outLen = executeResult == null ? 0 : executeResult.length();
            String preview = executeResult != null && executeResult.length() > 1000
                    ? executeResult.substring(0, 1000) + "..."
                    : (executeResult == null ? "" : executeResult);

            // 执行完成日志
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "Shell脚本执行完成",
                    Map.of(
                            "durationMs", durationMs,
                            "outputLength", outLen,
                            "outputPreview", preview
                    )
            ));

            // 结果写入输出
            outputContent.append(executeResult);
            output.put(node.getStepName() + ".output", executeResult);

            // 如果需要打印到前端/控制台，追加回调
            if (node.isPrint()) {
                try {
                    eventMessageCallbackMessage(executeResult);
                } catch (Exception e) {
                    log.warn("追加输出失败: {}", e.getMessage(), e);
                }
            }

            return CompletableFuture.completedFuture(null);

        } catch (Exception ex) {
            // 捕获并记录异常日志（包含堆栈）
            String errMsg = ex.getMessage() != null ? ex.getMessage() : "未知异常";
            String stack = StackTraceUtils.getStackTrace(ex);
            if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";

            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "ERROR",
                    "Shell脚本执行异常: " + errMsg,
                    Map.of(
                            "exception", errMsg,
                            "stackTrace", stack
                    )
            ));

            output.put(node.getStepName() + ".result", "Shell error: " + errMsg);

            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;

        }
    }

    private ShellNodeData getNodeData() {
        String nodeDataJson = String.valueOf(node.getProperties().get("node_data"));
        return JSONObject.parseObject(nodeDataJson, ShellNodeData.class);
    }

}
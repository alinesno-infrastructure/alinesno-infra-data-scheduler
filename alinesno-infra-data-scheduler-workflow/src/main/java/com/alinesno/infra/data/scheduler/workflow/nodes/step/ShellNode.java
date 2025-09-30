package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.PythonNodeData;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.ShellNodeData;
import com.alinesno.infra.data.scheduler.workflow.utils.OSUtils;
import com.alinesno.infra.data.scheduler.workflow.utils.shell.ShellHandle;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * 文本转语音节点（改为异步链签名）
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

    /**
     * 说明：
     * - 该实现为轻量同步执行并返回已完成的 CompletableFuture，
     *   因为 executeNode(...) 是由上层在 chatThreadPool 中调用的（FlowServiceImpl.executeFlowNode 已用 chatThreadPool 提交）。
     * - 若实际 TTS 需要远端调用或耗时合成，请把耗时部分改为异步（CompletableFuture.supplyAsync(..., ttsExecutor)）。
     */
    @Override
    protected CompletableFuture<Void> handleNode() {
        try {
            ShellNodeData nodeData = getNodeData();
            log.debug("TextToSpeechNode nodeData = {}", nodeData);
            log.debug("node type = {} output = {}", node.getType(), output);

            String rawScript = nodeData.getShellScript();

            log.debug("Shell Executor rawScript: {}", rawScript) ;

            // 构建多行命令行
            String command = """
                cd %s
                %s
                """.formatted(getWorkspace() , rawScript);

            runCommand(command);

            // 将结果写入 output（遵循原有约定）
            // output.put(node.getStepName() + ".result", result);

            // 若需要把内容追加到 outputContent（打印输出），可以判断 node.isPrint()
//            if (node.isPrint()) {
//                try {
//                    eventMessageCallbackMessage(result);
//                } catch (Exception e) {
//                    log.warn("追加输出失败: {}", e.getMessage(), e);
//                }
//            }

            // 轻量操作，直接返回已完成的 Future（在父线程池线程执行）
            return CompletableFuture.completedFuture(null);

        } catch (Exception ex) {
            log.error("TextToSpeechNode 执行异常: {}", ex.getMessage(), ex);
            // 将错误信息放到 output 中，便于后续流程判断
            output.put(node.getStepName() + ".result", "TTS error: " + ex.getMessage());
            // 将异常转换为已完成的 future（也可返回 completeExceptionally）
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;
        }
    }

    /**
     * 获取工作空间
     * @return
     */
    private String getWorkspace() {
        return null;
    }

    private ShellNodeData getNodeData() {
        String nodeDataJson = String.valueOf(node.getProperties().get("node_data"));
        return JSONObject.parseObject(nodeDataJson, ShellNodeData.class);
    }

    @SneakyThrows
    public void runCommand(String command) {
        File logFile = new File(getWorkspace(), PipeConstants.RUNNING_LOGGER);

        ShellHandle shellHandle;

        boolean isWindows = OSUtils.isWindows() ;
        if(isWindows){
            shellHandle = new ShellHandle("cmd.exe", "/C", command);
        }else if(OSUtils.isMacOS()){
            shellHandle = new ShellHandle("/bin/sh", "-c", command);
        }else{
            shellHandle = new ShellHandle("/bin/sh", "-c", command);
        }

        shellHandle.setLogPath(logFile.getAbsolutePath());

        shellHandle.execute();
    }
}
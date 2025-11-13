// PythonNode.java
package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.PythonNodeData;
import com.alinesno.infra.data.scheduler.workflow.utils.CommonsTextSecrets;
import com.alinesno.infra.data.scheduler.workflow.utils.SecretUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Python执行节点
 */
@Slf4j
@Data
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "python")
@EqualsAndHashCode(callSuper = true)
public class PythonNode extends AbstractFlowNode {

    public PythonNode() {
        setType("python");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {
        File pythonFile = null;
        try {
            // 1. 解析节点配置
            PythonNodeData nodeData = getNodeData();
            log.debug("PythonNode nodeData = {}", nodeData);

            // 1.1 解析配置日志
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "Python节点配置解析完成",
                    Map.of(
                            "script", nodeData.getPythonScript(),
                            "scriptPreview", nodeData.getPythonScript() ,
                            "scriptLength", nodeData.getPythonScript().length(),
                            "printEnable", node.isPrint()
                    )
            ));

            // 2. 生成临时Python文件
            String pythonScript = nodeData.getPythonScript();
            pythonFile = new File(getWorkspace(), "python_" + IdUtil.getSnowflakeNextIdStr() + ".py");

            String commandReplace = CommonsTextSecrets.replace(pythonScript , getOrgSecret()) ;
            FileUtils.writeStringToFile(pythonFile,commandReplace, Charset.defaultCharset(), false);

            // 检查命令中是否有未解析的密钥
            Set<String> unresolvedSecrets = SecretUtils.checkAndLogUnresolvedSecrets(commandReplace , node , flowExecution, nodeLogService) ;
            log.debug("未解析的密钥：{}" , unresolvedSecrets);

            // 2.1 文件生成日志
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "Python脚本文件生成成功",
                    Map.of(
                            "filePath", pythonFile.getAbsolutePath(),
                            "fileSize", pythonFile.length(),
                            "workspace", getWorkspace().getAbsolutePath()
                    )
            ));

            // 3. 执行Python脚本
            String executeOutput = runCommand("python3 " + pythonFile.getAbsolutePath());

            // 3.1 执行结果日志
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "Python脚本执行完成",
                    Map.of(
                            "output", executeOutput,
                            "outputLength", executeOutput.length(),
                            "outputPreview", executeOutput.length() > 500 ? executeOutput.substring(0, 500) + "..." : executeOutput
                    )
            ));

            // 4. 处理输出结果
            output.put(node.getStepName() + ".result", executeOutput);
            outputContent.append(executeOutput);

            if (node.isPrint()) {
                eventMessageCallbackMessage(executeOutput);
            }

            return CompletableFuture.completedFuture(null);

        } catch (Exception ex) {
            // 5. 异常日志
            String errorMsg = ex.getMessage() != null ? ex.getMessage() : "未知异常";
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "ERROR",
                    "Python节点执行失败: " + errorMsg,
                    Map.of(
                            "exception", errorMsg,
                            "filePath", pythonFile != null ? pythonFile.getAbsolutePath() : "N/A"
                    )
            ));

            output.put(node.getStepName() + ".result", "Python error: " + errorMsg);
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;

        } finally {
            // 6. 文件清理日志
            if (pythonFile != null && pythonFile.exists()) {
                boolean deleted = pythonFile.delete();
                nodeLogService.append(NodeLog.of(
                        flowExecution.getId().toString(),
                        node.getId(),
                        node.getStepName(),
                        deleted ? "INFO" : "WARN",
                        deleted ? "临时Python文件清理成功" : "临时Python文件清理失败",
                        Map.of(
                                "filePath", pythonFile.getAbsolutePath(),
                                "size", pythonFile.length()
                        )
                ));
            }
        }
    }

    private PythonNodeData getNodeData() {
        String nodeDataJson = String.valueOf(node.getProperties().get("node_data"));
        return JSONObject.parseObject(nodeDataJson, PythonNodeData.class);
    }
}
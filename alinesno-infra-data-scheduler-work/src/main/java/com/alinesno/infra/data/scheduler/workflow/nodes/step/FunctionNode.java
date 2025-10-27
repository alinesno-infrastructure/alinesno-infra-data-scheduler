package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.data.scheduler.service.IDataSourceService;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.FunctionNodeData;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 脚本功能节点（带日志增强版）
 */
@Slf4j
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "function")
@EqualsAndHashCode(callSuper = true)
public class FunctionNode extends AbstractFlowNode {

    @Autowired
    private IDataSourceService dataSourceService;

    public FunctionNode() {
        setType("function");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {
        String taskId = String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown");
        String stepId = node != null ? node.getId() : "unknown";
        String stepName = node != null ? node.getStepName() : "unknown";

        // 1. 节点开始日志
        try {
            nodeLogService.append(NodeLog.of(
                    taskId,
                    stepId,
                    stepName,
                    "INFO",
                    "Function节点开始执行",
                    Map.of(
                            "nodeType", "function",
                            "stepName", stepName
                    )
            ));
        } catch (Exception ignore) {
            log.warn("记录 Function 开始日志失败: {}", ignore.getMessage());
        }

        FunctionNodeData nodeData = getNodeData();
        log.debug("FunctionNodeData = {}", nodeData);

        if (nodeData == null) {
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "ERROR",
                        "Function节点配置为空",
                        Map.of("phase", "config.empty")
                ));
            } catch (Exception ignore) {
                log.warn("记录 config.empty 日志失败: {}", ignore.getMessage());
            }
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.supplyAsync(() -> {
            // 2. 脚本执行前日志
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "INFO",
                        "开始执行Groovy脚本",
                        Map.of(
                                "phase", "script.start",
                                "scriptLength", nodeData.getRawScript() == null ? 0 : nodeData.getRawScript().length(),
                                "hasReaderDs", nodeData.getReaderDataSourceId() != null,
                                "hasSinkDs", nodeData.getSinkDataSourceId() != null
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录 script.start 日志失败: {}", ignore.getMessage());
            }

            String scriptText = nodeData.getRawScript();
            return executeGroovyScript(scriptText, nodeData, outputContent);
        }).thenAccept(nodeOutput -> {
            // 3. 执行成功日志
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "INFO",
                        "Groovy脚本执行完成",
                        Map.of(
                                "phase", "script.complete",
                                "outputLength", nodeOutput == null ? 0 : nodeOutput.length(),
                                "outputPreview", nodeOutput != null && nodeOutput.length() > 200 ?
                                        nodeOutput.substring(0, 200) + "..." : nodeOutput
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录 script.complete 日志失败: {}", ignore.getMessage());
            }

            log.debug("handleNode nodeOutput : {}", nodeOutput);
            outputContent.append("脚本执行结果:").append(nodeOutput);
            eventNodeMessage(nodeOutput);
            output.put(node.getStepName() + ".result", nodeOutput);

            if (node.isPrint() && StringUtils.isNotEmpty(nodeOutput)) {
                eventMessageCallbackMessage(nodeOutput);
            }
        }).exceptionally(ex -> {
            // 4. 异常日志
            try {
                String stack = StackTraceUtils.getStackTrace(ex);
                if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";

                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "ERROR",
                        "Function节点执行异常: " + ex.getMessage(),
                        Map.of(
                                "phase", "execution.error",
                                "exception", ex.getMessage(),
                                "stackTrace", stack
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录 execution.error 日志失败: {}", ignore.getMessage());
            }

            log.error("FunctionNode 执行异常: {}", ex.getMessage(), ex);
            return null;
        });
    }

    private FunctionNodeData getNodeData() {
        try {
            String nodeDataJson = String.valueOf(node.getProperties().get("node_data"));
            return JSONObject.parseObject(nodeDataJson, FunctionNodeData.class);
        } catch (Exception e) {
            try {
                nodeLogService.append(NodeLog.of(
                        String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                        node.getId(),
                        node.getStepName(),
                        "ERROR",
                        "解析Function节点配置失败",
                        Map.of(
                                "phase", "config.parse",
                                "exception", e.getMessage()
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录 config.parse 日志失败: {}", ignore.getMessage());
            }
            return null;
        }
    }

    private String executeGroovyScript(String scriptText, FunctionNodeData nodeData, StringBuilder outputContent) {
        String taskId = String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown");
        String stepId = node != null ? node.getId() : "unknown";
        String stepName = node != null ? node.getStepName() : "unknown";

        PrintStream oldPrintStream = System.out;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bos));

        try {
            outputContent.append("脚本执行开始:").append(scriptText);

            // 3. 数据源绑定日志
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "DEBUG",
                        "绑定脚本变量",
                        Map.of(
                                "readerDsId", nodeData.getReaderDataSourceId(),
                                "sinkDsId", nodeData.getSinkDataSourceId()
                        )
                ));
            } catch (Exception ignore) {
                log.debug("记录数据源绑定日志失败: {}", ignore.getMessage());
            }

            Binding binding = new Binding();
            binding.setVariable("executorService", this);
            binding.setVariable("log", log);

            // 数据源绑定
            if (nodeData.getReaderDataSourceId() != null) {
                try {
                    DruidDataSource dataSource = dataSourceService.getDataSource(nodeData.getReaderDataSourceId());
                    if (dataSource != null) {
                        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                        binding.setVariable("readerDataSource", dataSource);
                        binding.setVariable("readerJdbcTemplate", jdbcTemplate);
                    } else {
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "WARN",
                                "读取数据源不存在",
                                Map.of("dsId", nodeData.getReaderDataSourceId())
                        ));
                    }
                } catch (Exception e) {
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "ERROR",
                            "读取数据源绑定失败",
                            Map.of(
                                    "dsId", nodeData.getReaderDataSourceId(),
                                    "exception", e.getMessage()
                            )
                    ));
                }
            }

            if (nodeData.getSinkDataSourceId() != null) {
                try {
                    DruidDataSource sinkDataSource = dataSourceService.getDataSource(nodeData.getSinkDataSourceId());
                    if (sinkDataSource != null) {
                        JdbcTemplate jdbcTemplate = new JdbcTemplate(sinkDataSource);
                        binding.setVariable("sinkDataSource", sinkDataSource);
                        binding.setVariable("sinkJdbcTemplate", jdbcTemplate);
                    } else {
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "WARN",
                                "写入数据源不存在",
                                Map.of("dsId", nodeData.getSinkDataSourceId())
                        ));
                    }
                } catch (Exception e) {
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "ERROR",
                            "写入数据源绑定失败",
                            Map.of(
                                    "dsId", nodeData.getSinkDataSourceId(),
                                    "exception", e.getMessage()
                            )
                    ));
                }
            }

            GroovyShell shell = new GroovyShell(FunctionNode.class.getClassLoader(), binding);

            long scriptStartTime = System.currentTimeMillis();
            Object resultObj = shell.evaluate(scriptText);
            long durationMs = System.currentTimeMillis() - scriptStartTime;

            // 4. 脚本执行详情日志
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "DEBUG",
                        "脚本执行完成",
                        Map.of(
                                "durationMs", durationMs,
                                "resultType", resultObj == null ? "null" : resultObj.getClass().getSimpleName()
                        )
                ));
            } catch (Exception ignore) {
                log.debug("记录脚本执行详情日志失败: {}", ignore.getMessage());
            }

            return resultObj == null ? "" : resultObj.toString();
        } catch (Exception e) {
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "ERROR",
                        "Groovy脚本执行异常",
                        Map.of(
                                "phase", "script.execute",
                                "exception", e.getMessage(),
                                "stackTrace", StackTraceUtils.getStackTrace(e).substring(0, 4000) + "..."
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录脚本执行异常日志失败: {}", ignore.getMessage());
            }
            throw new RuntimeException("脚本执行失败: " + e.getMessage(), e);
        } finally {
            System.setOut(oldPrintStream);
            String consoleOutput = bos.toString();
            if (StringUtils.isNotBlank(consoleOutput)) {
                try {
                    // 5. 控制台输出日志（截断长文本）
                    String preview = consoleOutput.length() > 1000 ?
                            consoleOutput.substring(0, 1000) + "..." : consoleOutput;
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "DEBUG",
                            "脚本控制台输出",
                            Map.of(
                                    "outputLength", consoleOutput.length(),
                                    "outputPreview", preview
                            )
                    ));
                } catch (Exception ignore) {
                    log.debug("记录控制台输出日志失败: {}", ignore.getMessage());
                }
            }
            outputContent.append("脚本执行结束:").append(bos.toString());
        }
    }
}
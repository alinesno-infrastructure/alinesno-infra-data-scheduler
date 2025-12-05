package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.FunctionNodeData;
import com.alinesno.infra.data.scheduler.workflow.service.IDataSourceService;
import com.alinesno.infra.data.scheduler.workflow.utils.CommonsTextSecrets;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 脚本功能节点（带日志增强版）
 */
@Slf4j
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "function")
@EqualsAndHashCode(callSuper = true)
public class FunctionNode extends AbstractFlowNode {

    private static final int OUT_TIME = 2 ; // 任务超时时间(小时)

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

            String scriptTextSec = nodeData.getRawScript();
            String scriptText = CommonsTextSecrets.replace(scriptTextSec, orgSecret) ;

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

            // 关键：记录完日志后重新抛出，确保外层 future 能感知到异常
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new CompletionException(ex);
            }
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
        // 获取当前任务和步骤的标识信息
        String taskId = String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown");
        String stepId = node != null ? node.getId() : "unknown";
        String stepName = node != null ? node.getStepName() : "unknown";

        // 用于跟踪需要关闭的数据源
        List<DruidDataSource> dataSourcesToClose = new ArrayList<>();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Object> future = null;

        try {
            // 3. 记录脚本开始执行的日志
            outputContent.append("脚本执行开始:").append(scriptText);

            // 4. 数据源绑定日志
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

            // 5. 初始化Groovy绑定变量
            Binding binding = new Binding();
            binding.setVariable("executorService", this);
            binding.setVariable("log", log); // 传递日志对象

            // 6. 绑定读取数据源（readerDataSource和readerJdbcTemplate）
            if (nodeData.getReaderDataSourceId() != null) {
                try {
                    DruidDataSource dataSource = dataSourceService.getDataSource(nodeData.getReaderDataSourceId());
                    if (dataSource != null) {
                        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                        binding.setVariable("readerDataSource", dataSource);
                        binding.setVariable("readerJdbcTemplate", jdbcTemplate);

                        // 添加到待关闭列表
                        dataSourcesToClose.add(dataSource);
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

            // 7. 绑定写入数据源（sinkDataSource和sinkJdbcTemplate）
            if (nodeData.getSinkDataSourceId() != null) {
                try {
                    DruidDataSource sinkDataSource = dataSourceService.getDataSource(nodeData.getSinkDataSourceId());
                    if (sinkDataSource != null) {
                        JdbcTemplate jdbcTemplate = new JdbcTemplate(sinkDataSource);
                        binding.setVariable("sinkDataSource", sinkDataSource);
                        binding.setVariable("sinkJdbcTemplate", jdbcTemplate);

                        // 添加到待关闭列表
                        dataSourcesToClose.add(sinkDataSource);
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

            // 8. 执行Groovy脚本（通过 Executor + Future 控制超时）
            Callable<Object> task = () -> {
                GroovyShell shell = new GroovyShell(FunctionNode.class.getClassLoader(), binding);
                return shell.evaluate(scriptText); // 执行脚本
            };

            long scriptStartTime = System.currentTimeMillis();
            future = executor.submit(task);

            Object resultObj;
            try {
                // 设置超时时间
                resultObj = future.get(OUT_TIME , TimeUnit.HOURS);
            } catch (TimeoutException te) {
                // 取消任务并记录超时日志
                future.cancel(true);
                String msg = "Groovy脚本执行超时（>"+OUT_TIME+"小时）";
                try {
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "ERROR",
                            msg,
                            Map.of("phase", "script.timeout")
                    ));
                } catch (Exception ignore) {
                    log.warn("记录 script.timeout 日志失败: {}", ignore.getMessage());
                }
                throw new RuntimeException(msg, te);
            }
            long durationMs = System.currentTimeMillis() - scriptStartTime;

            // 9. 记录脚本执行详情日志
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

            // 10. 返回脚本执行结果
            return resultObj == null ? "" : resultObj.toString();

        } catch (Exception e) {
            // 11. 捕获脚本执行异常并记录
            try {
                String stack = StackTraceUtils.getStackTrace(e);
                if (stack.length() > 4000) {
                    stack = stack.substring(0, 4000) + "...";
                }
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "ERROR",
                        "Groovy脚本执行异常",
                        Map.of(
                                "phase", "script.execute",
                                "exception", e.getMessage(),
                                "stackTrace", stack
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录脚本执行异常日志失败: {}", ignore.getMessage());
            }
            throw new RuntimeException("脚本执行失败: " + e.getMessage(), e);

        } finally {
            // 尝试平滑关闭 executor（若未关闭）
            try {
                if (future != null && !future.isDone()) {
                    future.cancel(true);
                }
                executor.shutdownNow();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (Exception ignore) {
                log.debug("关闭脚本执行线程池失败: {}", ignore.getMessage());
            }

            // 关闭所有已绑定且未关闭的数据源
            for (DruidDataSource dataSource : dataSourcesToClose) {
                try {
                    if (!dataSource.isClosed()) {
                        dataSource.close();
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "DEBUG",
                                "数据源已关闭",
                                Map.of("dsId", dataSource.getName())
                        ));
                    }
                } catch (Exception e) {
                    log.warn("关闭数据源失败: {}", e.getMessage());
                    try {
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "WARN",
                                "关闭数据源失败",
                                Map.of(
                                        "dsId", dataSource.getName(),
                                        "exception", e.getMessage()
                                )
                        ));
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
            }

            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "DEBUG",
                        "脚本完整输出（含错误）",
                        Map.of()
                ));
            } catch (Exception ignore) {
                log.debug("记录脚本完整输出日志失败: {}", ignore.getMessage());
            }
        }
    }

}
package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.FunctionNodeData;
import com.alinesno.infra.data.scheduler.workflow.service.IDataSourceService;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import com.alinesno.infra.data.scheduler.workflow.utils.ThreadLocalStreamHolder;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
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

    /**
     * 线程隔离的实时输出流，仅捕获当前线程的输出并写入nodeLogService
     */
    class RealTimeByteArrayOutputStream extends ByteArrayOutputStream {
        private final PrintStream originalStream; // 当前线程的原始输出流（System.out或System.err）
        private final String taskId;
        private final String stepId;
        private final String stepName;
        private final boolean isErrorStream; // 标识是否为错误流（System.err）
        private final StringBuilder lineBuffer = new StringBuilder(); // 缓存单行未完成的输出

        /**
         * 构造方法
         * @param originalStream 原始输出流（当前线程的System.out或System.err）
         * @param taskId 任务ID
         * @param stepId 步骤ID
         * @param stepName 步骤名称
         * @param isErrorStream 是否为错误流
         */
        public RealTimeByteArrayOutputStream(PrintStream originalStream, String taskId, String stepId, String stepName, boolean isErrorStream) {
            this.originalStream = originalStream;
            this.taskId = taskId;
            this.stepId = stepId;
            this.stepName = stepName;
            this.isErrorStream = isErrorStream;
        }

        /**
         * 重写写入方法，实时转发并解析内容
         */
        @Override
        public void write(byte[] b, int off, int len) {
            super.write(b, off, len); // 写入缓冲区（用于最终合并）
            originalStream.write(b, off, len); // 实时转发到原始输出流（控制台）
            originalStream.flush(); // 强制刷新，确保实时输出

            // 解析字节为字符串，按行处理
            String content = new String(b, off, len, StandardCharsets.UTF_8);
            processContent(content);
        }

        /**
         * 按行分割内容，完整行写入nodeLog
         */
        private void processContent(String content) {
            int start = 0;
            int end;
            while ((end = content.indexOf('\n', start)) != -1) {
                // 提取一行（包含换行符）
                String line = content.substring(start, end + 1);
                lineBuffer.append(line);
                // 清除行尾的换行符和回车符，避免日志冗余
                String cleanLine = lineBuffer.toString().replaceAll("[\r\n]+$", "");
                // 写入nodeLogService
                writeToNodeLog(cleanLine);
                lineBuffer.setLength(0); // 清空缓冲区
                start = end + 1;
            }
            // 处理剩余未换行的内容（缓存到下一次写入）
            if (start < content.length()) {
                lineBuffer.append(content.substring(start));
            }
        }

        /**
         * 将单行内容写入nodeLogService
         */
        private void writeToNodeLog(String line) {
            try {
                // 根据是否为错误流设置日志级别（错误流为ERROR，普通流为INFO）
                String level = isErrorStream ? "ERROR" : "INFO";
                String message = isErrorStream ? "脚本错误输出" : "脚本实时输出";
                String phase = isErrorStream ? "script.error" : "script.realtime";

                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        level,
                        message,
                        Map.of(
                                "phase", phase,
                                "content", line
                        )
                ));
            } catch (Exception e) {
                // 记录日志失败时不影响主流程，仅本地打印警告
                log.warn("记录实时输出日志失败: {}", e.getMessage());
            }
        }

        /**
         * 重写刷新方法，确保最后一行未完成的内容被写入
         */
        @Override
        public void flush() throws IOException {
            super.flush();
            originalStream.flush(); // 同步刷新原始输出流
            // 若缓冲区有未完成的行，强制写入
            if (!lineBuffer.isEmpty()) {
                writeToNodeLog(lineBuffer.toString());
                lineBuffer.setLength(0);
            }
        }
    }

//    private String executeGroovyScript(String scriptText, FunctionNodeData nodeData, StringBuilder outputContent) {
//        String taskId = String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown");
//        String stepId = node != null ? node.getId() : "unknown";
//        String stepName = node != null ? node.getStepName() : "unknown";
//
////        PrintStream oldPrintStream = System.out;
////        ByteArrayOutputStream bos = new ByteArrayOutputStream();
////        System.setOut(new PrintStream(bos));
//
//        // 保存原有的out和err
//        PrintStream oldOut = System.out;
//        PrintStream oldErr = System.err;
//
////        // 替换原输出流初始化代码
////        PrintStream oldPrintStream = System.out;
////
////        // 传入任务信息，用于实时日志记录
////        RealTimeByteArrayOutputStream bos = new RealTimeByteArrayOutputStream(
////                oldPrintStream,
////                taskId,  // 从方法参数获取
////                stepId,  // 从方法参数获取
////                stepName // 从方法参数获取
////        );
////
////        System.setOut(new PrintStream(bos));
//
//        // 重定向System.out（普通输出流，isErrorStream=false）
//        RealTimeByteArrayOutputStream outBos = new RealTimeByteArrayOutputStream(oldOut, taskId, stepId, stepName, false);
//        System.setOut(new PrintStream(outBos));
//
//        // 重定向System.err（错误输出流，isErrorStream=true）
//        RealTimeByteArrayOutputStream errBos = new RealTimeByteArrayOutputStream(oldErr, taskId, stepId, stepName, true);
//        System.setErr(new PrintStream(errBos));
//
//        try {
//
//            outputContent.append("脚本执行开始:").append(scriptText);
//
//            // 3. 数据源绑定日志
//            try {
//                nodeLogService.append(NodeLog.of(
//                        taskId,
//                        stepId,
//                        stepName,
//                        "DEBUG",
//                        "绑定脚本变量",
//                        Map.of(
//                                "readerDsId", nodeData.getReaderDataSourceId(),
//                                "sinkDsId", nodeData.getSinkDataSourceId()
//                        )
//                ));
//            } catch (Exception ignore) {
//                log.debug("记录数据源绑定日志失败: {}", ignore.getMessage());
//            }
//
//            Binding binding = new Binding();
//            binding.setVariable("executorService", this);
//            binding.setVariable("log", log);
//
//            // 数据源绑定
//            if (nodeData.getReaderDataSourceId() != null) {
//                try {
//                    DruidDataSource dataSource = dataSourceService.getDataSource(nodeData.getReaderDataSourceId());
//                    if (dataSource != null) {
//                        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
//                        binding.setVariable("readerDataSource", dataSource);
//                        binding.setVariable("readerJdbcTemplate", jdbcTemplate);
//                    } else {
//                        nodeLogService.append(NodeLog.of(
//                                taskId,
//                                stepId,
//                                stepName,
//                                "WARN",
//                                "读取数据源不存在",
//                                Map.of("dsId", nodeData.getReaderDataSourceId())
//                        ));
//                    }
//                } catch (Exception e) {
//                    nodeLogService.append(NodeLog.of(
//                            taskId,
//                            stepId,
//                            stepName,
//                            "ERROR",
//                            "读取数据源绑定失败",
//                            Map.of(
//                                    "dsId", nodeData.getReaderDataSourceId(),
//                                    "exception", e.getMessage()
//                            )
//                    ));
//                }
//            }
//
//            if (nodeData.getSinkDataSourceId() != null) {
//                try {
//                    DruidDataSource sinkDataSource = dataSourceService.getDataSource(nodeData.getSinkDataSourceId());
//                    if (sinkDataSource != null) {
//                        JdbcTemplate jdbcTemplate = new JdbcTemplate(sinkDataSource);
//                        binding.setVariable("sinkDataSource", sinkDataSource);
//                        binding.setVariable("sinkJdbcTemplate", jdbcTemplate);
//                    } else {
//                        nodeLogService.append(NodeLog.of(
//                                taskId,
//                                stepId,
//                                stepName,
//                                "WARN",
//                                "写入数据源不存在",
//                                Map.of("dsId", nodeData.getSinkDataSourceId())
//                        ));
//                    }
//                } catch (Exception e) {
//                    nodeLogService.append(NodeLog.of(
//                            taskId,
//                            stepId,
//                            stepName,
//                            "ERROR",
//                            "写入数据源绑定失败",
//                            Map.of(
//                                    "dsId", nodeData.getSinkDataSourceId(),
//                                    "exception", e.getMessage()
//                            )
//                    ));
//                }
//            }
//
//            GroovyShell shell = new GroovyShell(FunctionNode.class.getClassLoader(), binding);
//
//            long scriptStartTime = System.currentTimeMillis();
//            Object resultObj = shell.evaluate(scriptText);
//            long durationMs = System.currentTimeMillis() - scriptStartTime;
//
//            // 4. 脚本执行详情日志
//            try {
//                nodeLogService.append(NodeLog.of(
//                        taskId,
//                        stepId,
//                        stepName,
//                        "DEBUG",
//                        "脚本执行完成",
//                        Map.of(
//                                "durationMs", durationMs,
//                                "resultType", resultObj == null ? "null" : resultObj.getClass().getSimpleName()
//                        )
//                ));
//            } catch (Exception ignore) {
//                log.debug("记录脚本执行详情日志失败: {}", ignore.getMessage());
//            }
//
//            return resultObj == null ? "" : resultObj.toString();
//        } catch (Exception e) {
//            try {
//                nodeLogService.append(NodeLog.of(
//                        taskId,
//                        stepId,
//                        stepName,
//                        "ERROR",
//                        "Groovy脚本执行异常",
//                        Map.of(
//                                "phase", "script.execute",
//                                "exception", e.getMessage(),
//                                "stackTrace", StackTraceUtils.getStackTrace(e).substring(0, 4000) + "..."
//                        )
//                ));
//            } catch (Exception ignore) {
//                log.warn("记录脚本执行异常日志失败: {}", ignore.getMessage());
//            }
//            throw new RuntimeException("脚本执行失败: " + e.getMessage(), e);
//        } finally {
//            // 恢复原有的out和err
//            System.setOut(oldOut);
//            System.setErr(oldErr);
//
//            // 合并out和err的输出（可选，根据需要是否在最终日志中合并）
//            String outContent = outBos.toString();
//            String errContent = errBos.toString();
//            String consoleOutput = outContent + "\n" + errContent;
//
//            // 原有最终日志记录逻辑...
//            if (StringUtils.isNotBlank(consoleOutput)) {
//                try {
//                    String preview = consoleOutput.length() > 1000 ? consoleOutput.substring(0, 1000) + "..." : consoleOutput;
//                    nodeLogService.append(NodeLog.of(
//                            taskId,
//                            stepId,
//                            stepName,
//                            "DEBUG",
//                            "脚本完整输出（含错误）",
//                            Map.of(
//                                    "outputLength", consoleOutput.length(),
//                                    "outputPreview", preview
//                            )
//                    ));
//                } catch (Exception ignore) {
//                    log.debug("记录完整输出日志失败: {}", ignore.getMessage());
//                }
//            }
//            outputContent.append("脚本执行结束:").append(consoleOutput);
//
//        }
//    }

    private String executeGroovyScript(String scriptText, FunctionNodeData nodeData, StringBuilder outputContent) {
        // 获取当前任务和步骤的标识信息
        String taskId = String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown");
        String stepId = node != null ? node.getId() : "unknown";
        String stepName = node != null ? node.getStepName() : "unknown";

        // 定义当前线程的输出流（仅对当前线程生效）
        RealTimeByteArrayOutputStream outBos = null;
        RealTimeByteArrayOutputStream errBos = null;

        try {
            // 1. 创建当前线程的输出流（关联原始System.out和System.err，但不全局替换）
            outBos = new RealTimeByteArrayOutputStream(System.out, taskId, stepId, stepName, false);
            errBos = new RealTimeByteArrayOutputStream(System.err, taskId, stepId, stepName, true);

            // 2. 仅对当前线程重定向System.out和System.err（通过ThreadLocal隔离）
            ThreadLocalStreamHolder.redirect(new PrintStream(outBos), new PrintStream(errBos));

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

            // 8. 执行Groovy脚本
            GroovyShell shell = new GroovyShell(FunctionNode.class.getClassLoader(), binding);
            long scriptStartTime = System.currentTimeMillis();
            Object resultObj = shell.evaluate(scriptText); // 执行脚本
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
            // 12. 恢复当前线程的原始输出流（关键：不影响其他线程）
            ThreadLocalStreamHolder.restore();

            // 13. 合并当前线程的输出内容（System.out + System.err）
            String outContent = (outBos != null) ? outBos.toString() : "";
            String errContent = (errBos != null) ? errBos.toString() : "";
            String consoleOutput = outContent + "\n" + errContent;

            // 14. 记录完整的控制台输出日志（截断长文本）
            if (StringUtils.isNotBlank(consoleOutput)) {
                try {
                    String preview = consoleOutput.length() > 1000 ?
                            consoleOutput.substring(0, 1000) + "..." : consoleOutput;
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "DEBUG",
                            "脚本完整输出（含错误）",
                            Map.of(
                                    "outputLength", consoleOutput.length(),
                                    "outputPreview", preview
                            )
                    ));
                } catch (Exception ignore) {
                    log.debug("记录控制台输出日志失败: {}", ignore.getMessage());
                }
            }

            // 15. 保存完整输出到结果中
            outputContent.append("脚本执行结束:").append(consoleOutput);
        }
    }
}
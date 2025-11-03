// AbstractWorkflowNode.java
package com.alinesno.infra.data.scheduler.workflow.nodes;

import com.agentsflex.core.llm.Llm;
import com.agentsflex.core.message.AiMessage;
import com.agentsflex.core.message.MessageStatus;
import com.agentsflex.core.util.StringUtil;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.api.worker.FlowNodeDto;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.llm.service.ILlmModelService;
import com.alinesno.infra.data.scheduler.workflow.WorkflowManage;
import com.alinesno.infra.data.scheduler.workflow.constants.AgentConstants;
import com.alinesno.infra.data.scheduler.entity.worker.FlowExecutionEntity;
import com.alinesno.infra.data.scheduler.entity.worker.FlowNodeExecutionEntity;
import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.logger.NodeLogService;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.GlobalVariables;
import com.alinesno.infra.data.scheduler.workflow.parse.TextReplacer;
import com.alinesno.infra.data.scheduler.workflow.utils.CommonsTextSecrets;
import com.alinesno.infra.data.scheduler.workflow.utils.OSUtils;
import com.alinesno.infra.data.scheduler.workflow.utils.SecretUtils;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import com.alinesno.infra.data.scheduler.workflow.utils.shell.ShellHandle;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * 这是一个抽象父类，继承自 WorkflowNode 接口。
 * 它作为所有具体工作流节点类的基类，包含了所有工作流节点可能通用的属性和方法。
 * 由于它是抽象类，不能直接实例化，具体的工作流节点类需要继承该类并实现必要的逻辑。
 * 使用 Lombok 的 @Data 注解自动生成 getter、setter、toString、equals 和 hashCode 方法。
 */
@Slf4j
@Setter
public abstract class AbstractFlowNode implements FlowNode {

    @Autowired
    protected ILlmModelService llmModelService;

    @Autowired
    @Qualifier("chatThreadPool")
    protected ThreadPoolTaskExecutor chatThreadPool;

    @Autowired
    protected NodeLogService nodeLogService;

    /**
     * 工作流管理类，用于管理流程中的节点和数据
     */
    protected WorkflowManage workflowManage;

    /**
     * 节点列表，用于存储流程中的所有节点
     */
    protected List<FlowNodeDto> flowNodes ;

    /**
     * 流程节点DTO，用于存储流程节点的相关信息
     */
    protected FlowNodeDto node;

    /**
     * 最后输出的内容
     */
    protected StringBuilder outputContent ;

    /**
     * 流程执行实体，代表整个流程的执行实例
     */
    protected FlowExecutionEntity flowExecution;

    /**
     * 流程节点执行实体，记录流程中每个节点的执行情况
     */
    protected FlowNodeExecutionEntity flowNodeExecution;

    /**
     * 输出参数的集合，用于存储流程节点执行后的输出数据
     */
    protected Map<String, Object> output;

    /**
     * 原始密钥
     */
    @Getter
    protected Map<String, String> orgSecret;

    /**
     * 节点类型
     */
    private String type ;

    /**
     * 全局变量
     */
    private GlobalVariables globalVariables;

    // AbstractFlowNode.java
    @SneakyThrows
    public CompletableFuture<Void> executeNode(
            FlowNodeDto node,
            FlowExecutionEntity flowExecution,
            FlowNodeExecutionEntity flowNodeExecution,
            Map<String, Object> output,
            StringBuilder outputContent,
            Map<String, String> orgSecret) {

        // 1. 初始化工作流参数（同步准备，无阻塞逻辑）
        workflowManage = new WorkflowManage(node, flowNodes);
        boolean isPrintContent = isPrintContent(node);
        node.setPrint(isPrintContent);

        nodeLogService.setOrgSecret(orgSecret);

        // 设置成员变量（同步操作）
        this.setNode(node);
        this.orgSecret = orgSecret;
        this.outputContent = outputContent;
        this.flowExecution = flowExecution;
        this.flowNodeExecution = flowNodeExecution;
        this.output = output;

        log.debug("执行节点任务:{} , node:{}", node.getType(), node.getProperties());
        eventStepMessage(AgentConstants.STEP_START); // 发送开始事件

        // 2. 处理全局变量（仅 start 节点需要，同步逻辑）
        if ("start".equals(node.getType())) {

            // 1. 节点开始执行日志
            NodeLog startLog = NodeLog.of(
                    flowExecution.getId().toString(), // taskId：使用流程执行ID（flowExecution.getId() 为 Long 类型）
                    node.getId(), // nodeId：节点界面ID（node.getId() 为 String 类型）
                    node.getStepName(), // nodeName：节点名称
                    "INFO", // 日志级别
                    "节点开始执行", // 消息摘要
                    Map.of(
                            "nodeType", node.getType(), // 节点类型（如 "start"、"llm"等）
                            "flowExecutionStatus", flowExecution.getExecutionStatus(), // 流程执行状态
                            "stepId", node.getId(), // 节点界面ID（冗余但便于查询）
                            "runUniqueNumber", flowExecution.getRunUniqueNumber() // 流程运行唯一号（便于追踪单次执行）
                    )
            );
            nodeLogService.append(startLog); // 调用 NodeLogService 记录日志


            List<String> messages = new ArrayList<>();
            GlobalVariables globalVariables = new GlobalVariables(null , null , messages);
            this.setGlobalVariables(globalVariables);
            // 填充全局变量到 output（同步操作）
            output.put("global.time", globalVariables.getTime());

        }

        workflowManage.setOutput(output);

        // 3. 异步执行节点逻辑（核心改造：调用子类异步 handleNode()）
        return handleNode()
                .thenRun(() -> {

                    // 5. 节点输出结果日志
                    NodeLog outputLog = NodeLog.of(
                            flowExecution.getId().toString(),
                            node.getId(),
                            node.getStepName(),
                            "INFO",
                            "节点输出参数记录",
                            Map.of(
                                    "outputKeys", output.keySet().toString(), // 输出参数键列表（如 ["result", "summary"]）
                                    "outputSize", output.size(),
                                    "isLastNode", node.isLastNode(), // 是否为最后一个节点
                                    "contentLength", outputContent.length() // 输出内容总长度
                            )
                    );
                    nodeLogService.append(outputLog);

                    // 4. 节点成功后处理（异步回调）
                    log.debug("output = {}", output);
                    eventStepMessage(AgentConstants.STEP_FINISH); // 发送完成事件
                    // 如果是最后节点，异步保存完整输出
                    if (node.isLastNode()) {
                        log.debug("保存完整输出");

                        // 节点执行成功：记录结束日志
                        long durationMs = System.currentTimeMillis() - flowNodeExecution.getExecuteTime().getTime(); // 计算耗时（假设 executeTime 为节点开始执行时间）

                        NodeLog endLog = NodeLog.of(
                                flowExecution.getId().toString(),
                                node.getId(),
                                node.getStepName(),
                                "INFO",
                                "节点执行成功",
                                Map.of(
                                        "nodeType", node.getType(),
                                        "executionStatus", "SUCCESS",
                                        "durationMs", durationMs, // 耗时（毫秒）
                                        "outputKeys", output.keySet().toString(), // 输出参数键列表（摘要）
                                        "outputPreview", outputContent .length() > 200 ?
                                                outputContent.substring(0, 200) + "..." : outputContent.toString() // 输出内容预览（截断长文本）
                                )
                        );
                        nodeLogService.append(endLog);

                    }
                })
                .exceptionally(ex -> {

                    // 节点执行异常：记录错误日志
                    String errorMsg = ex.getMessage() != null ? ex.getMessage() : "未知异常";
                    String stackTrace = StackTraceUtils.getStackTrace(ex); // 需要工具类获取堆栈（见下方工具类）

                    NodeLog errorLog = NodeLog.of(
                            flowExecution.getId().toString(),
                            node.getId(),
                            node.getStepName(),
                            "ERROR", // 错误级别
                            "节点执行失败: " + errorMsg,
                            Map.of(
                                    "nodeType", node.getType(),
                                    "executionStatus", "FAILED",
                                    "exception", errorMsg,
                                    "stackTrace", stackTrace.length() > 5000 ? stackTrace.substring(0, 5000) + "..." : stackTrace, // 堆栈截断（避免过大）
                                    "flowExecutionId", flowExecution.getId(),
                                    "flowNodeExecutionId", flowNodeExecution.getId() // 节点执行记录ID（便于关联数据库中的执行记录）
                            )
                    );
                    nodeLogService.append(errorLog);

                    // 5. 异常处理（异步回调）
                    log.error("节点执行失败:{}", ex.getMessage(), ex);
                    eventStepMessage("节点执行失败: " + ex.getMessage(), AgentConstants.STEP_ERROR);

                    // return null; // 异常不中断上层流程，仅标记错误状态，向上层抛出异常

                    throw new CompletionException(ex);
                });
    }

    /**
     * 流程节点消息
     * @param status
     */
    protected void eventStepMessage(String status) {
        eventStepMessage("流程:" +node.getStepName(),  status);
    }

    /**
     * 流程节点消息
     * @param message
     * @param status
     */
    protected void eventStepMessage(String message , String status) {
        eventStepMessage(message ,  status , node.getId());
    }

    public boolean isPrintContent(FlowNodeDto dto) {
        // 设置isPrint
        Object nodeData = dto.getProperties().get("node_data") ;
        if(nodeData != null){
            JSONObject nodeDataJson = JSONObject.parseObject(nodeData.toString()) ;
            if (Objects.nonNull(nodeDataJson) && nodeDataJson.get("isPrint") != null) {
                try {
                    return nodeDataJson.getBoolean("isPrint");
                }catch (Exception e){
                    log.error("判断是否打印输出异常:{}" , e.getMessage());
                }
            }
        }
        return false ;
    }


    /**
     * 流程节点消息
     * @param message
     * @param status
     * @param stepId
     */
    protected void eventStepMessage(String message , String status , String stepId) {
//        flowExpertService.eventStepMessage(message ,  status , stepId);
    }

    /**
     * 运行事件消息
     * @param msg
     */
    protected void eventMessage(String msg) {
//        streamMessagePublisher.doStuffAndPublishAnEvent(msg ,
//                role,
//                taskInfo,
//                taskInfo.getFlowChatId()
//        );

    }


    /**
     * 保存所有消息
     * @param msg
     */
    @SneakyThrows
    protected void eventMessageCallbackMessage(String msg) {

        outputContent.append(msg) ;  // 每个节点的内容都添加到outputContent中
        outputContent.append("\n\n"); // 添加换行

    }

    /**
     * 处理节点任务
     */
    protected abstract CompletableFuture<Void> handleNode();

    protected void eventNodeMessage(String newMsg) {
       eventNodeMessage(newMsg , null);
    }

    protected void eventNodeMessage(String newMsg , String reasoningText) {

//        // 如果两个都为空，则没有需要处理的信息，直接返回
//        if ((newMsg == null || newMsg.isEmpty()) &&
//                (reasoningText == null || reasoningText.isEmpty())) {
//            return;
//        }
//
//        FlowStepStatusDto stepDto = new FlowStepStatusDto() ;
//        stepDto.setMessage("任务进行中...") ;
//        stepDto.setStepId(node.getId()) ;
//        stepDto.setStatus(AgentConstants.STEP_PROCESS);
//
//        if(StringUtils.hasLength(newMsg)){
//            stepDto.setFlowChatText(newMsg) ;
//        }
//
//        if(StringUtils.hasLength(reasoningText)){
//            stepDto.setFlowReasoningText(reasoningText);
//        }
//
//        stepDto.setPrint(node.isPrint());
//
//        taskInfo.setFlowStep(stepDto);
//
//        streamMessagePublisher.doStuffAndPublishAnEvent(null , //  msg.substring(preMsg.toString().length()),
//                role,
//                taskInfo,
//                taskInfo.getFlowChatId());
    }

    protected CompletableFuture<String> getAiChatResultAsync(Llm llm, String prompt) {
        final CompletableFuture<String> future = new CompletableFuture<>();

        long aiStartTime = System.currentTimeMillis(); // AI调用开始时间

        // 4.1 AI调用开始日志
        NodeLog aiStartLog = NodeLog.of(
                flowExecution.getId().toString(),
                node.getId(),
                node.getStepName(),
                "INFO",
                "AI模型调用开始",
                Map.of(
                        "llmModel", llm.toString() , // AI模型名称（假设 Llm 类有 getName() 方法）
                        "promptLength", prompt.length(), // prompt长度（摘要）
                        "promptPreview", prompt.length() > 100 ? prompt.substring(0, 100) + "..." : prompt // prompt摘要
                )
        );
        nodeLogService.append(aiStartLog);

        try {
            llm.chatStream(prompt, (context, response) -> {
                try {
                    AiMessage message = response.getMessage();
                    if (message == null) {
                        return;
                    }

                    // 实时片段推送（与之前逻辑一致）
                    if (StringUtil.hasText(message.getReasoningContent())) {

                        NodeLog reasoningLog = NodeLog.of(
                                flowExecution.getId().toString(),
                                node.getId(),
                                node.getStepName(),
                                "INFO",
                                "AI推理内容生成",
                                Map.of(
                                        "reasoningPreview", message.getReasoningContent().length() > 200 ?
                                                message.getReasoningContent().substring(0, 200) + "..." : message.getReasoningContent(),
                                        "status", "STREAMING" // 流式推理中
                                )
                        );
                        nodeLogService.append(reasoningLog);

                        eventNodeMessage(null , message.getReasoningContent());
                    }

                    // 实时片段推送（与之前逻辑一致）
                    if (StringUtil.hasText(message.getContent())) {
                        eventNodeMessage(message.getContent());
                    }

                    // 终止时完成 future
                    if (message.getStatus() == MessageStatus.END) {
                        String full = message.getFullContent();

                        long aiDurationMs = System.currentTimeMillis() - aiStartTime;
                        NodeLog aiEndLog = NodeLog.of(
                                flowExecution.getId().toString(),
                                node.getId(),
                                node.getStepName(),
                                "INFO",
                                "AI模型调用结束",
                                Map.of(
                                        "llmModel", llm.toString(),
                                        "responseLength", message.getFullContent().length(),
                                        "durationMs", aiDurationMs,
                                        "status", "COMPLETED"
                                )
                        );
                        nodeLogService.append(aiEndLog);

                        future.complete(full);
                    }
                } catch (Exception ex) {

                    NodeLog aiErrorLog = NodeLog.of(
                            flowExecution.getId().toString(),
                            node.getId(),
                            node.getStepName(),
                            "ERROR",
                            "AI模型调用异常: " + ex.getMessage(),
                            Map.of(
                                    "llmModel", llm.toString(),
                                    "exception", ex.getMessage(),
                                    "stackTrace", StackTraceUtils.getStackTrace(ex).substring(0, 5000) + "..."
                            )
                    );
                    nodeLogService.append(aiErrorLog);

                    future.completeExceptionally(ex);
                }
            });
        } catch (Exception e) {

            NodeLog aiInitErrorLog = NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "ERROR",
                    "AI模型调用初始化失败: " + e.getMessage(),
                    Map.of(
                            "llmModel", llm.toString(),
                            "exception", e.getMessage()
                    )
            );
            nodeLogService.append(aiInitErrorLog);

            future.completeExceptionally(e);
        }

        // 为避免无限等待，可在调用处或这里设置超时
        // 例如：return future.orTimeout(120, TimeUnit.SECONDS);
        return future;
    }

    /**
     * 替换占位符
     * @return
     */
    protected String replacePlaceholders(String text){
       return TextReplacer.replacePlaceholders(text, output);
    }


    @SneakyThrows
    protected String runCommand(String command) {
        File logFile = new File(getWorkspace(), PipeConstants.RUNNING_LOGGER);

        ShellHandle shellHandle;
        boolean isWindows = OSUtils.isWindows();
        String commandReplace = CommonsTextSecrets.replace(command ,getOrgSecret()) ;
        if (isWindows) {
            shellHandle = new ShellHandle("cmd.exe", "/C",commandReplace );
        } else {
            shellHandle = new ShellHandle("/bin/sh", "-c", commandReplace) ;
        }

        // 可根据环境选择流编码（例如 Windows 中文环境可能使用 GBK）
        // shellHandle.setStreamCharset(java.nio.charset.Charset.forName("GBK"));
        // 默认为 UTF-8（也可以按需设置为 GBK）
        shellHandle.setLogPath(logFile.getAbsolutePath());

        // 检查命令中是否有未解析的密钥
        Set<String> unresolvedSecrets = SecretUtils.checkAndLogUnresolvedSecrets(commandReplace , node , flowExecution, nodeLogService) ;
        log.debug("未解析的密钥：{}" ,unresolvedSecrets);

        long startTs = System.currentTimeMillis();
        // 记录开始日志（元信息）
        String commandPreview = command.length() > 200 ? command.substring(0, 200) + "..." : command;
        try {
            nodeLogService.append(NodeLog.of(
                    String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                    node != null ? node.getId() : "unknown",
                    node != null ? node.getStepName() : "unknown",
                    "INFO",
                    "命令开始执行",
                    Map.of(
                            "phase", "runCommand.start",
                            "commandPreview", commandPreview,
                            "logFile", logFile.getAbsolutePath(),
                            "workspace", getWorkspace().getAbsolutePath()
                    )
            ));
        } catch (Exception ignore) {
            log.warn("记录 runCommand start 日志失败: {}", ignore.getMessage());
        }

        // 为实时上报准备缓冲与锁（线程安全）
        final StringBuilder buf = new StringBuilder();
        final Object bufLock = new Object();
        final int FLUSH_THRESHOLD = 4 * 1024; // 达到 4KB 时强制 flush（可调整）
        final int FLUSH_LINE_TRIGGER = 1; // 只要检测到换行就 flush（配合 fragment 中可能包含换行）

        // 注册 listener：shell 每次 writeLog 会回调这里的 onLogFragment（可能来自 stdout 或 stderr）
        shellHandle.setLogListener(fragment -> {
            if (fragment == null || fragment.isEmpty()) return;
            try {
                synchronized (bufLock) {
                    buf.append(fragment);
                    // 如果 fragment 中包含换行或缓冲区超过阈值则发送
                    boolean hasNewline = fragment.indexOf('\n') >= 0 || fragment.indexOf('\r') >= 0;
                    if (hasNewline || buf.length() > FLUSH_THRESHOLD) {
                        // 构建要发送的日志片段并清空缓冲
                        String toSend = buf.toString();
                        buf.setLength(0);
                        try {
                            nodeLogService.append(NodeLog.of(
                                    String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                                    node != null ? node.getId() : "unknown",
                                    node != null ? node.getStepName() : "unknown",
                                    "INFO",
                                    "命令输出片段",
                                    Map.of(
                                            "phase", "runCommand.stream",
                                            "commandPreview", commandPreview,
                                            "logFragmentPreview", toSend.length() > 2000 ? toSend.substring(0,2000) + "..." : toSend,
                                            "logFile", logFile.getAbsolutePath()
                                    )
                            ));
                        } catch (Exception e) {
                            // nodeLogService 异常单独记录，不影响主流程
                            log.warn("实时上报日志到 nodeLogService 失败: {}", e.getMessage());
                        }
                    }
                }
            } catch (Throwable t) {
                // listener 内部异常不影响主流程
                log.warn("日志 listener 处理失败: {}", t.getMessage());
            }
        });

        String output;
        try {
            // 执行命令（ShellHandle 会把 stdout/stderr 分别调用 writeLog，writeLog 会触发 listener）
            shellHandle.execute();

            // shellHandle.getOutput() 返回 stdout 的全部内容（注意：如果输出很大会占内存）
            output = shellHandle.getOutput();

            // 执行完成后 flush 剩余缓冲，确保没有残留
            synchronized (bufLock) {
                if (!buf.isEmpty()) {
                    String toSend = buf.toString();
                    buf.setLength(0);
                    try {
                        nodeLogService.append(NodeLog.of(
                                String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                                node != null ? node.getId() : "unknown",
                                node != null ? node.getStepName() : "unknown",
                                "INFO",
                                "命令输出片段(结束flush)",
                                Map.of(
                                        "phase", "runCommand.stream",
                                        "commandPreview", commandPreview,
                                        "logFragmentPreview", toSend.length() > 2000 ? toSend.substring(0,2000) + "..." : toSend,
                                        "logFile", logFile.getAbsolutePath()
                                )
                        ));
                    } catch (Exception e) {
                        log.warn("flush 时上报日志失败: {}", e.getMessage());
                    }
                }
            }

            long durationMs = System.currentTimeMillis() - startTs;
            int outLen = output == null ? 0 : output.length();

            // 输出 preview（如果很大只保留预览）
            String preview = "";
            if (output != null) {
                preview = output.length() > 1000 ? output.substring(0, 1000) + "..." : output;
            }

            nodeLogService.append(NodeLog.of(
                    String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                    node != null ? node.getId() : "unknown",
                    node != null ? node.getStepName() : "unknown",
                    "INFO",
                    "命令执行完成",
                    Map.of(
                            "phase", "runCommand.end",
                            "commandPreview", commandPreview,
                            "durationMs", durationMs,
                            "outputLength", outLen,
                            "outputPreview", preview,
                            "logFile", logFile.getAbsolutePath()
                    )
            ));

            return output;
        } catch (Exception ex) {
            long durationMs = System.currentTimeMillis() - startTs;
            String errMsg = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            String stack = StackTraceUtils.getStackTrace(ex);
            if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";

            // 出错时先尝试 flush 缓冲
            synchronized (bufLock) {
                if (!buf.isEmpty()) {
                    String toSend = buf.toString();
                    buf.setLength(0);
                    try {
                        nodeLogService.append(NodeLog.of(
                                String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                                node != null ? node.getId() : "unknown",
                                node != null ? node.getStepName() : "unknown",
                                "ERROR",
                                "命令执行异常时输出片段",
                                Map.of(
                                        "phase", "runCommand.stream.error",
                                        "commandPreview", commandPreview,
                                        "logFragmentPreview", toSend.length() > 2000 ? toSend.substring(0,2000) + "..." : toSend,
                                        "logFile", logFile.getAbsolutePath()
                                )
                        ));
                    } catch (Exception ignore) {
                        log.warn("异常处理时 flush 上报失败: {}", ignore.getMessage());
                    }
                }
            }

            try {
                nodeLogService.append(NodeLog.of(
                        String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                        node != null ? node.getId() : "unknown",
                        node != null ? node.getStepName() : "unknown",
                        "ERROR",
                        "命令执行异常: " + errMsg,
                        Map.of(
                                "phase", "runCommand.error",
                                "commandPreview", commandPreview,
                                "durationMs", durationMs,
                                "exception", errMsg,
                                "stackTrace", stack,
                                "logFile", logFile.getAbsolutePath()
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录 runCommand error 日志失败: {}", ignore.getMessage());
            }
            throw ex;
        }
    }

    /**
     * 系统临时目录
     * @return
     */
    protected File getWorkspace() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

}
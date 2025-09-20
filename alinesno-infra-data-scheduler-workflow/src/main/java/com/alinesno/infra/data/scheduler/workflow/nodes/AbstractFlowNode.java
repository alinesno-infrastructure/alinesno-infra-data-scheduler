// AbstractWorkflowNode.java
package com.alinesno.infra.data.scheduler.workflow.nodes;

import com.agentsflex.core.llm.Llm;
import com.agentsflex.core.message.AiMessage;
import com.agentsflex.core.message.MessageStatus;
import com.agentsflex.core.util.StringUtil;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.llm.service.ILlmModelService;
import com.alinesno.infra.data.scheduler.workflow.WorkflowManage;
import com.alinesno.infra.data.scheduler.workflow.constants.AgentConstants;
import com.alinesno.infra.data.scheduler.workflow.dto.FlowNodeDto;
import com.alinesno.infra.data.scheduler.workflow.entity.FlowExecutionEntity;
import com.alinesno.infra.data.scheduler.workflow.entity.FlowNodeExecutionEntity;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.GlobalVariables;
import com.alinesno.infra.data.scheduler.workflow.parse.TextReplacer;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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

//    /**
//     * 流程专家服务，提供流程相关的业务逻辑处理服务
//     */
//    protected FlowExpertService flowExpertService;

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
            StringBuilder outputContent) {

        // 1. 初始化工作流参数（同步准备，无阻塞逻辑）
        workflowManage = new WorkflowManage(node, flowNodes);
        boolean isPrintContent = isPrintContent(node);
        node.setPrint(isPrintContent);

//        flowExpertService.setNode(node);
//        flowExpertService.setOutputContent(outputContent);

        // 设置成员变量（同步操作）
        this.setNode(node);
        this.outputContent = outputContent;
        this.flowExecution = flowExecution;
        this.flowNodeExecution = flowNodeExecution;
        this.output = output;

        log.debug("执行节点任务:{} , node:{}", node.getType(), node.getProperties());
        eventStepMessage(AgentConstants.STEP_START); // 发送开始事件

        // 2. 处理全局变量（仅 start 节点需要，同步逻辑）
        if ("start".equals(node.getType())) {
            List<String> messages = new ArrayList<>();
//            handleHistoryMessage(messages, taskInfo.getChannelId());
//            String preContent = workflowExecution == null ? "" : workflowExecution.getContent();
            GlobalVariables globalVariables = new GlobalVariables(null , null , messages);
            this.setGlobalVariables(globalVariables);
            // 填充全局变量到 output（同步操作）
            output.put("global.time", globalVariables.getTime());
//            output.put("global.pre_content", globalVariables.getPreContent());
//            output.put("global.channelId", globalVariables.getChannelId());
//            output.put("global.history_content", String.join(",", globalVariables.getHistoryContent()));
//            output.put("global.datasetKnowledgeDocument", StringUtils.hasLength(taskInfo.getDatasetKnowledgeDocument()) ? taskInfo.getDatasetKnowledgeDocument() : "");

//            if (taskInfo.getAttachments() != null && !taskInfo.getAttachments().isEmpty()) {
//                output.put("global.document", taskInfo.getAttachments().get(0));
//            }

        }

        workflowManage.setOutput(output);

        // 3. 异步执行节点逻辑（核心改造：调用子类异步 handleNode()）
        return handleNode()
                .thenRun(() -> {
                    // 4. 节点成功后处理（异步回调）
                    log.debug("output = {}", output);
                    eventStepMessage(AgentConstants.STEP_FINISH); // 发送完成事件
                    // 如果是最后节点，异步保存完整输出
                    if (node.isLastNode()) {
                        log.debug("保存完整输出");
                    }
                })
                .exceptionally(ex -> {
                    // 5. 异常处理（异步回调）
                    log.error("节点执行失败:{}", ex.getMessage(), ex);
                    eventStepMessage("节点执行失败: " + ex.getMessage(), AgentConstants.STEP_ERROR);
                    return null; // 异常不中断上层流程，仅标记错误状态
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
            if (Objects.nonNull(nodeDataJson)) {
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

        try {
            llm.chatStream(prompt, (context, response) -> {
                try {
                    AiMessage message = response.getMessage();
                    if (message == null) {
                        return;
                    }

                    // 实时片段推送（与之前逻辑一致）
                    if (StringUtil.hasText(message.getReasoningContent())) {
//                        taskInfo.setReasoningText(null);
                        eventNodeMessage(null , message.getReasoningContent());
                    }

                    // 实时片段推送（与之前逻辑一致）
                    if (StringUtil.hasText(message.getContent())) {
//                        taskInfo.setReasoningText(null);
                        eventNodeMessage(message.getContent());
                    }

                    // 终止时完成 future
                    if (message.getStatus() == MessageStatus.END) {
                        String full = message.getFullContent();
                        // 持久化最终消息（保持之前行为）
//                        MessageEntity entity = new MessageEntity();
//                        entity.setTraceBusId(taskInfo.getTraceBusId());
//                        entity.setId(IdUtil.getSnowflakeNextId());
//                        entity.setContent(full);
//                        entity.setReasoningContent(message.getFullReasoningContent());
//                        entity.setFormatContent(full);
//                        entity.setName(role.getRoleName());
//                        entity.setRoleType("agent");
//                        entity.setReaderType("html");
//                        entity.setAddTime(new Date());
//                        entity.setIcon(role.getRoleAvatar());
//                        entity.setChannelId(taskInfo.getChannelId());
//                        entity.setRoleId(role.getId());
//                        // 保存操作可能是同步的，建议交给 orchestratorExecutor/异步写入以避免阻塞回调线程
//                        try {
//                            flowExpertService.getMessageService().save(entity);
//                        } catch (Exception ex) {
//                            log.warn("保存消息实体异常: {}", ex.getMessage(), ex);
//                        }

                        future.complete(full);
                    }
                } catch (Exception ex) {
                    future.completeExceptionally(ex);
                }
            });
        } catch (Exception e) {
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

}
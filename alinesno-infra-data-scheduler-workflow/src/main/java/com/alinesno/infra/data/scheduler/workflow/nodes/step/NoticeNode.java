package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.data.scheduler.entity.NotificationConfigEntity;
import com.alinesno.infra.data.scheduler.im.bean.NotificationMessage;
import com.alinesno.infra.data.scheduler.im.bean.NotificationResult;
import com.alinesno.infra.data.scheduler.im.enums.NotificationType;
import com.alinesno.infra.data.scheduler.im.service.INotificationConfigService;
import com.alinesno.infra.data.scheduler.im.service.INotificationService;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.NoticeNodeData;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 通知节点（带 nodeLogService 日志增强）
 */
@Slf4j
@Data
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "notice")
@EqualsAndHashCode(callSuper = true)
public class NoticeNode extends AbstractFlowNode {

    @Autowired
    private INotificationConfigService notificationConfigService;

    @Autowired
    private INotificationService notificationService;

    public NoticeNode() {
        setType("notice");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {
        String taskId = String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown");
        String stepId = node != null ? node.getId() : "unknown";
        String stepName = node != null ? node.getStepName() : "unknown";
        NotificationConfigEntity notificationConfigEntity = null;

        // 开始执行日志
        try {
            nodeLogService.append(NodeLog.of(
                    taskId,
                    stepId,
                    stepName,
                    "INFO",
                    "Notice 节点开始执行",
                    Map.of("nodeType", "notice")
            ));
        } catch (Exception ignore) {
            log.warn("记录 Notice 开始日志失败: {}", ignore.getMessage());
        }

        NoticeNodeData nodeData = getAiChatProperties();
        log.debug("节点数据 = {}", nodeData);

        if (nodeData == null) {
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "WARN",
                        "Notice 节点配置为空，跳过执行",
                        Map.of("phase", "config.empty")
                ));
            } catch (Exception ignore) {
                log.warn("记录 config.empty 日志失败: {}", ignore.getMessage());
            }
            return CompletableFuture.completedFuture(null);
        }

        String imId = nodeData.getImId();
        try {
            notificationConfigEntity = notificationConfigService.getById(imId);
        } catch (Exception ex) {
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "ERROR",
                        "获取通知配置失败: " + imId,
                        Map.of("phase", "config.fetch", "exception", ex.getMessage())
                ));
            } catch (Exception ignore) {
                log.warn("记录 config.fetch 日志失败: {}", ignore.getMessage());
            }
            log.error("获取通知配置失败 imId={}", imId, ex);
            return CompletableFuture.completedFuture(null);
        }

        if (notificationConfigEntity == null) {
            log.warn("未找到通知配置，imId={}", imId);
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "WARN",
                        "通知配置未找到",
                        Map.of("imId", imId)
                ));
            } catch (Exception ignore) {
                log.warn("记录 config.missing 日志失败: {}", ignore.getMessage());
            }
            return CompletableFuture.completedFuture(null);
        }

        if (Boolean.FALSE.equals(notificationConfigEntity.getEnabled())) {
            log.warn("通知配置 {} 已禁用，跳过发送", notificationConfigEntity.getId());
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "INFO",
                        "通知配置已禁用，跳过发送",
                        Map.of("configId", notificationConfigEntity.getId())
                ));
            } catch (Exception ignore) {
                log.warn("记录 config.disabled 日志失败: {}", ignore.getMessage());
            }
            return CompletableFuture.completedFuture(null);
        }

        // 构建消息
        NotificationMessage message = new NotificationMessage();
        try {
            message.setConfigId(notificationConfigEntity.getId());

            NotificationType type;
            try {
                type = NotificationType.valueOf(notificationConfigEntity.getProvider());
                message.setType(type);
            } catch (Exception ex) {
                log.warn("未知的通知提供者 '{}'，配置 id {}，跳过发送", notificationConfigEntity.getProvider(), notificationConfigEntity.getId(), ex);
                try {
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "WARN",
                            "未知的通知提供者，跳过发送",
                            Map.of("provider", notificationConfigEntity.getProvider(), "configId", notificationConfigEntity.getId())
                    ));
                } catch (Exception ignore) {
                    log.warn("记录 provider.invalid 日志失败: {}", ignore.getMessage());
                }
                return CompletableFuture.completedFuture(null);
            }

            String content = nodeData.getNoticeContent();
            content = replacePlaceholders(content);

            if (StringUtils.isNotEmpty(content)) {
                message.setContent(content);
                String title = content.length() > 64 ? content.substring(0, 64) : content;
                message.setTitle(title);
            } else {
                message.setTitle("调度通知");
                message.setContent("来自调度的通知（无详细内容）");
            }

            // 可在此设置接收人：message.setTos(...)
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "DEBUG",
                        "构建通知消息完成",
                        Map.of(
                                "configId", notificationConfigEntity.getId(),
                                "provider", notificationConfigEntity.getProvider(),
                                "titlePreview", message.getTitle(),
                                "contentPreview", message.getContent(),
                                "contentLength", message.getContent().length()
                        )
                ));
            } catch (Exception ignore) {
                log.debug("记录 message.build 日志失败: {}", ignore.getMessage());
            }

        } catch (Exception ex) {
            log.error("为 imId={} 构建通知消息失败", imId, ex);
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "ERROR",
                        "构建通知消息失败",
                        Map.of("imId", imId, "exception", ex.getMessage())
                ));
            } catch (Exception ignore) {
                log.warn("记录 message.build.exception 日志失败: {}", ignore.getMessage());
            }
            return CompletableFuture.completedFuture(null);
        }

        // 异步发送并支持重试
        int maxRetries = Math.max(0, nodeData.getRetryCount());
        int retryBackoffBaseMs = 1000; // 基础退避
        NotificationConfigEntity finalNotificationConfigEntity = notificationConfigEntity;

        return CompletableFuture.runAsync(() -> {
            int attempt = 0;
            boolean sent = false;
            while (!sent) {
                attempt++;
                try {
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "INFO",
                            "尝试发送通知",
                            Map.of("configId", finalNotificationConfigEntity.getId(), "attempt", attempt)
                    ));
                } catch (Exception ignore) {
                    log.debug("记录 send.attempt 日志失败: {}", ignore.getMessage());
                }

                try {
                    NotificationResult result = notificationService.send(message);
                    if (result != null && result.isSuccess()) {
                        sent = true;
                        log.info("通知发送成功，configId={}，尝试次数={}", finalNotificationConfigEntity.getId(), attempt);
                        outputContent.append("通知发送成功，尝试次数:").append(attempt);
                        try {
                            nodeLogService.append(NodeLog.of(
                                    taskId,
                                    stepId,
                                    stepName,
                                    "INFO",
                                    "通知发送成功",
                                    Map.of("configId", finalNotificationConfigEntity.getId(), "attempt", attempt)
                            ));
                        } catch (Exception ignore) {
                            log.debug("记录 send.success 日志失败: {}", ignore.getMessage());
                        }
                        break;
                    } else {
                        String errMsg = result == null ? "返回结果为空" : result.getMessage();
                        log.warn("通知发送失败，configId={}，尝试次数={}，错误={}", finalNotificationConfigEntity.getId(), attempt, errMsg);
                        outputContent.append("通知发送失败：").append(errMsg);
                        try {
                            nodeLogService.append(NodeLog.of(
                                    taskId,
                                    stepId,
                                    stepName,
                                    "WARN",
                                    "通知发送失败",
                                    Map.of("configId", finalNotificationConfigEntity.getId(), "attempt", attempt, "error", errMsg)
                            ));
                        } catch (Exception ignore) {
                            log.debug("记录 send.failure 日志失败: {}", ignore.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    String stack = StackTraceUtils.getStackTrace(ex);
                    if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";
                    log.error("发送通知时发生异常，configId={}，尝试次数={}", finalNotificationConfigEntity.getId(), attempt, ex);
                    outputContent.append("发送通知时发生异常：").append(ex.getMessage());
                    try {
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "ERROR",
                                "发送通知发生异常",
                                Map.of("configId", finalNotificationConfigEntity.getId(), "attempt", attempt, "exception", ex.getMessage(), "stackTrace", stack)
                        ));
                    } catch (Exception ignore) {
                        log.debug("记录 send.exception 日志失败: {}", ignore.getMessage());
                    }
                }

                if (attempt > maxRetries) {
                    log.error("通知重试次数耗尽，configId={}，总尝试次数={}", finalNotificationConfigEntity.getId(), attempt);
                    outputContent.append("通知重试次数耗尽");
                    try {
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "ERROR",
                                "通知重试次数耗尽",
                                Map.of("configId", finalNotificationConfigEntity.getId(), "attempts", attempt)
                        ));
                    } catch (Exception ignore) {
                        log.debug("记录 retry.exhausted 日志失败: {}", ignore.getMessage());
                    }
                    break;
                }

                // 退避等待
                try {
                    long sleepMs = Math.min(60_000L, retryBackoffBaseMs * attempt * 2L);
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("通知重试被中断，configId={}", finalNotificationConfigEntity.getId());
                    outputContent.append("通知重试被中断");
                    try {
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "WARN",
                                "通知重试被中断",
                                Map.of("configId", finalNotificationConfigEntity.getId())
                        ));
                    } catch (Exception ignore) {
                        log.debug("记录 retry.interrupt 日志失败: {}", ignore.getMessage());
                    }
                    break;
                }
            }

            // 最终状态记录
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "INFO",
                        "Notice 节点执行结束",
                        Map.of("configId", finalNotificationConfigEntity.getId(), "sent", sent)
                ));
            } catch (Exception ignore) {
                log.debug("记录 Notice 完成日志失败: {}", ignore.getMessage());
            }

        }, chatThreadPool);
    }

    private NoticeNodeData getAiChatProperties(){
        String nodeDataJson =  node.getProperties().get("node_data")+"" ;
        return StringUtils.isNotEmpty(nodeDataJson)? JSONObject.parseObject(nodeDataJson , NoticeNodeData.class):null;
    }
}
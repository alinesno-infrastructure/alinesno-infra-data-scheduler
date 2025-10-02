package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.data.scheduler.entity.NotificationConfigEntity;
import com.alinesno.infra.data.scheduler.im.bean.NotificationMessage;
import com.alinesno.infra.data.scheduler.im.enums.NotificationType;
import com.alinesno.infra.data.scheduler.im.service.INotificationConfigService;
import com.alinesno.infra.data.scheduler.im.service.INotificationService;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.NoticeNodeData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 节点类型为 "notice" 的节点类。
 * 主要功能是根据历史聊天记录优化完善当前问题，使问题更利于匹配知识库中的分段内容。
 * 在工作流中，当需要提高问题与知识库匹配度时，会使用该节点。
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

    /**
     * 构造函数，初始化节点类型为 "question"。
     */
    public NoticeNode() {
        setType("notice");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {
        NoticeNodeData nodeData = getAiChatProperties();
        log.debug("节点数据 = {}", nodeData);

        if (nodeData == null) {
            return CompletableFuture.completedFuture(null);
        }

        String imId = nodeData.getImId();
        NotificationConfigEntity notificationConfigEntity = notificationConfigService.getById(imId);

        log.debug("通知配置实体 = {}", notificationConfigEntity);

        if (notificationConfigEntity == null) {
            log.warn("未找到通知配置，imId={}", imId);
            return CompletableFuture.completedFuture(null);
        }

        // 如果配置显式禁用，则不发送
        if (Boolean.FALSE.equals(notificationConfigEntity.getEnabled())) {
            log.warn("通知配置 {} 已禁用，跳过发送", notificationConfigEntity.getId());
            return CompletableFuture.completedFuture(null);
        }

        // 组装消息
        NotificationMessage message = new NotificationMessage();
        try {
            // 设置 config id
            message.setConfigId(notificationConfigEntity.getId());

            // provider -> NotificationType
            try {
                NotificationType type = NotificationType.valueOf(notificationConfigEntity.getProvider());
                message.setType(type);
            } catch (Exception ex) {
                log.warn("未知的通知提供者 '{}'，配置 id {}，跳过发送", notificationConfigEntity.getProvider(), notificationConfigEntity.getId(), ex);
                return CompletableFuture.completedFuture(null);
            }

            // 内容与标题
            String content = nodeData.getNoticeContent();
            if (StringUtils.isNotEmpty(content)) {
                message.setContent(content);
                // 简单取前缀作为 title（防止 title 过长）
                String title = content.length() > 64 ? content.substring(0, 64) : content;
                message.setTitle(title);
            } else {
                message.setTitle("调度通知");
                message.setContent("来自调度的通知（无详细内容）");
            }

            // 若需要指定接收者，可在 nodeData 或 config 中扩展并设置 message.setTos(...)
        } catch (Exception ex) {
            log.error("为 imId={} 构建通知消息失败", imId, ex);
            return CompletableFuture.completedFuture(null);
        }

        // 异步发送并支持重试
        int maxRetries = Math.max(0, nodeData.getRetryCount());
        return CompletableFuture.runAsync(() -> {
            int attempt = 0;
            while (true) {
                attempt++;
                try {
                    com.alinesno.infra.data.scheduler.im.bean.NotificationResult result = notificationService.send(message);
                    if (result != null && result.isSuccess()) {
                        log.info("通知发送成功，configId={}，尝试次数={}", notificationConfigEntity.getId(), attempt);
                        break;
                    } else {
                        String errMsg = result == null ? "返回结果为空" : result.getMessage();
                        log.warn("通知发送失败，configId={}，尝试次数={}，错误={}", notificationConfigEntity.getId(), attempt, errMsg);
                    }
                } catch (Exception ex) {
                    log.error("发送通知时发生异常，configId={}，尝试次数={}", notificationConfigEntity.getId(), attempt, ex);
                }

                if (attempt > maxRetries) {
                    log.error("通知重试次数耗尽，configId={}，总尝试次数={}", notificationConfigEntity.getId(), attempt);
                    break;
                }

                // 简单退避：每次等待增长（最多 60s）
                try {
                    long sleepMs = Math.min(60_000L, 1000L * attempt * 2);
                    Thread.sleep(sleepMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("通知重试被中断，configId={}", notificationConfigEntity.getId());
                    break;
                }
            }
        });
    }

    private NoticeNodeData getAiChatProperties(){
        String nodeDataJson =  node.getProperties().get("node_data")+"" ;
        return StringUtils.isNotEmpty(nodeDataJson)? JSONObject.parseObject(nodeDataJson , NoticeNodeData.class):null;
    }

}
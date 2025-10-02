// NotificationStrategy.java
package com.alinesno.infra.data.scheduler.im.strategy;

import com.alinesno.infra.data.scheduler.entity.NotificationConfigEntity;
import com.alinesno.infra.data.scheduler.im.bean.NotificationMessage;
import com.alinesno.infra.data.scheduler.im.bean.NotificationResult;
import com.alinesno.infra.data.scheduler.im.enums.NotificationType;

/**
 * 通知策略接口，定义了不同类型通知的统一处理接口
 * 该接口用于实现各种通知方式的策略模式，包括邮件、短信、微信等通知类型
 */
public interface NotificationStrategy {

    /**
     * 获取通知策略对应的类型
     *
     * @return NotificationType 通知类型枚举值，标识该策略处理的通知类型
     */
    NotificationType type();

    /**
     * 发送通知消息
     *
     * @param message 通知消息对象，包含消息内容、接收人等信息
     * @param config 通知配置实体，包含发送通知所需的配置参数
     * @return NotificationResult 通知发送结果，包含发送状态、错误信息等结果信息
     */
    NotificationResult send(NotificationMessage message, NotificationConfigEntity config);
}

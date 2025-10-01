// NotificationStrategy.java
package com.alinesno.infra.data.scheduler.im.strategy;

import com.alinesno.infra.data.scheduler.entity.NotificationConfigEntity;
import com.alinesno.infra.data.scheduler.im.bean.NotificationMessage;
import com.alinesno.infra.data.scheduler.im.bean.NotificationResult;
import com.alinesno.infra.data.scheduler.im.enums.NotificationType;

public interface NotificationStrategy {
    NotificationType type();
    NotificationResult send(NotificationMessage message, NotificationConfigEntity config);
}
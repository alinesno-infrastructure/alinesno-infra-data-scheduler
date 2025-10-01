// INotificationService.java
package com.alinesno.infra.data.scheduler.im.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.entity.NotificationEntity;
import com.alinesno.infra.data.scheduler.im.bean.NotificationMessage;
import com.alinesno.infra.data.scheduler.im.bean.NotificationResult;

public interface INotificationService extends IBaseService<NotificationEntity> {
    NotificationResult send(NotificationMessage message);
}
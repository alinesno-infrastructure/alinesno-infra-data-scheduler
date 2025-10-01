package com.alinesno.infra.data.scheduler.im.service;

import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.entity.NotificationConfigEntity;

import java.io.Serializable;
import java.util.List;

public interface INotificationConfigService extends IBaseService<NotificationConfigEntity> {

    /**
     * 获取解密后的配置（用于编辑）
     */
    NotificationConfigEntity getDecryptedById(Serializable id);

    /**
     * 根据 provider 获取已启用配置列表（返回脱敏后的副本，用于列表/下拉）
     */
    List<NotificationConfigEntity> listByProvider(String provider, PermissionQuery query);

}
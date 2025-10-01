package com.alinesno.infra.data.scheduler.im.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.data.scheduler.entity.NotificationConfigEntity;
import com.alinesno.infra.data.scheduler.im.mapper.NotificationConfigMapper;
import com.alinesno.infra.data.scheduler.im.service.INotificationConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * 通知配置服务实现（包含加/解密与脱敏）
 */
@Slf4j
@Service
public class NotificationConfigServiceImpl extends IBaseServiceImpl<NotificationConfigEntity, NotificationConfigMapper>
        implements INotificationConfigService {

    // 需要脱敏 / 加密的字段列表（按实体字段名）
    private static final String MASK = "******";

    public NotificationConfigServiceImpl() {
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdate(NotificationConfigEntity entity) {
        if (entity == null) {
            return false;
        }
        validateConfig(entity);
//        encryptSensitive(entity);
        return super.saveOrUpdate(entity);
    }

    /**
     * 返回解密后的副本（用于编辑页面）
     */
    @Override
    public NotificationConfigEntity getDecryptedById(Serializable id) {
        NotificationConfigEntity stored = super.getById(id);
        if (stored == null) return null;
        NotificationConfigEntity copy = new NotificationConfigEntity();
        BeanUtils.copyProperties(stored, copy);
        return copy;
    }

    /**
     * 列表接口返回脱敏后的副本集合（不解密，统一掩码）
     */
    @Override
    public List<NotificationConfigEntity> listByProvider(String provider, PermissionQuery query) {
        LambdaQueryWrapper<NotificationConfigEntity> qw = new LambdaQueryWrapper<>();
        qw.setEntityClass(NotificationConfigEntity.class) ;
        if (provider != null && !provider.trim().isEmpty()) {
            qw.eq(NotificationConfigEntity::getProvider, provider.trim());
        }
        qw.eq(NotificationConfigEntity::getEnabled, true);
        query.toWrapper(qw);

        List<NotificationConfigEntity> list = baseMapper.selectList(qw);
        return list.stream().map(stored -> {
            NotificationConfigEntity copy = new NotificationConfigEntity();
            BeanUtils.copyProperties(stored, copy);
            return copy;
        }).collect(Collectors.toList());
    }

    /**
     * 基本校验逻辑（和之前一样）
     */
    private void validateConfig(NotificationConfigEntity cfg) {
        if (cfg == null) {
            throw new IllegalArgumentException("config empty");
        }
        if (cfg.getProvider() == null) {
            throw new IllegalArgumentException("provider required");
        }
        String p = cfg.getProvider().trim().toUpperCase();
        switch (p) {
            case "EMAIL" -> {
                if (isBlank(cfg.getSmtpHost())) {
                    throw new IllegalArgumentException("smtpHost required for EMAIL provider");
                }
                if (isBlank(cfg.getFromAddress())) {
                    throw new IllegalArgumentException("fromAddress required for EMAIL provider");
                }
            }
            case "WECHAT_ROBOT", "DINGTALK" -> {
                if (isBlank(cfg.getWebhook())) {
                    throw new IllegalArgumentException("webhook required for robot provider");
                }
            }
            default -> {
            }
        }
    }

}
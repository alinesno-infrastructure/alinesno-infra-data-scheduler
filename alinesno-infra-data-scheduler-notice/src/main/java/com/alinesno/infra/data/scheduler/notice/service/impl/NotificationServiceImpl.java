// NotificationServiceImpl.java
package com.alinesno.infra.data.scheduler.notice.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.scheduler.entity.NotificationConfigEntity;
import com.alinesno.infra.data.scheduler.entity.NotificationEntity;
import com.alinesno.infra.data.scheduler.notice.NotificationStrategyFactory;
import com.alinesno.infra.data.scheduler.notice.bean.NotificationMessage;
import com.alinesno.infra.data.scheduler.notice.bean.NotificationResult;
import com.alinesno.infra.data.scheduler.notice.mapper.NotificationConfigMapper;
import com.alinesno.infra.data.scheduler.notice.mapper.NotificationMapper;
import com.alinesno.infra.data.scheduler.notice.service.INotificationService;
import com.alinesno.infra.data.scheduler.notice.strategy.NotificationStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class NotificationServiceImpl extends IBaseServiceImpl<NotificationEntity , NotificationMapper> implements INotificationService {

    private final NotificationConfigMapper configMapper;
    private final NotificationStrategyFactory strategyFactory;

    public NotificationServiceImpl(NotificationConfigMapper configMapper,
                                   NotificationStrategyFactory strategyFactory) {
        this.configMapper = configMapper;
        this.strategyFactory = strategyFactory;
    }

    @Override
    @Transactional
    public NotificationResult send(NotificationMessage message) {
        // 将请求记录入库（PENDING）
        NotificationEntity record = new NotificationEntity();
        record.setConfigId(message.getConfigId());
        record.setProvider(message.getType().name());
        record.setTitle(message.getTitle());
        record.setContent(message.getContent());
        record.setTos(message.getTos() == null ? null : String.join(",", message.getTos()));
        record.setStatus("PENDING");
        record.setAddTime(new Date());
        this.baseMapper.insert(record);

        // 获取配置
        NotificationConfigEntity config = configMapper.selectById(message.getConfigId());
        if (config == null) {
            record.setStatus("FAILED");
            record.setErrorMsg("config not found");
            record.setUpdateTime(new Date());
            this.baseMapper.updateById(record);
            return NotificationResult.fail("CONFIG_NOT_FOUND", "Notification config not found", null);
        }

        // 选择策略并发送
        NotificationStrategy strategy = strategyFactory.getStrategy(message.getType());
        if (strategy == null) {
            record.setStatus("FAILED");
            record.setErrorMsg("strategy not implemented for type: " + message.getType());
            record.setAddTime(new Date());
            this.baseMapper.updateById(record);
            return NotificationResult.fail("STRATEGY_MISSING", "No strategy for type " + message.getType(), null);
        }

        NotificationResult result = strategy.send(message, config);

        // 更新记录
        record.setResponse(result.getRawResponse());
        record.setUpdateTime(new Date());
        if (result.isSuccess()) {
            record.setStatus("SUCCESS");
            this.baseMapper.updateById(record);
        } else {
            record.setStatus("FAILED");
            record.setErrorMsg(result.getMessage());
            this.baseMapper.updateById(record);
        }
        return result;
    }
}
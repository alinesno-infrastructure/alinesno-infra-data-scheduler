// NotificationStrategyFactory.java
package com.alinesno.infra.data.scheduler.im;

import com.alinesno.infra.data.scheduler.im.enums.NotificationType;
import com.alinesno.infra.data.scheduler.im.strategy.NotificationStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NotificationStrategyFactory {

    private final Map<NotificationType, NotificationStrategy> strategyMap;

    public NotificationStrategyFactory(List<NotificationStrategy> strategies) {
        this.strategyMap = strategies.stream().collect(Collectors.toMap(NotificationStrategy::type, s -> s));
    }

    public NotificationStrategy getStrategy(NotificationType type) {
        return strategyMap.get(type);
    }
}
package com.alinesno.infra.data.scheduler.workflow.logger.event;

import com.alinesno.infra.data.scheduler.workflow.logger.NodeLogSseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NodeLogEventListener {

    private final NodeLogSseService sseService;

    @Autowired
    public NodeLogEventListener(NodeLogSseService sseService) {
        this.sseService = sseService;
    }

    @EventListener
    public void onNodeLogCreated(NodeLogCreatedEvent event) {
        sseService.publish(event.getNodeLog());
    }
}
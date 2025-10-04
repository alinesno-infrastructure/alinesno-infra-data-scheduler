package com.alinesno.infra.data.scheduler.workflow.logger.event;

import com.alinesno.infra.data.scheduler.workflow.logger.NodeLog;

/**
 * 节点日志创建事件
 */
public class NodeLogCreatedEvent {

    private final NodeLog nodeLog;

    public NodeLogCreatedEvent(NodeLog nodeLog) { this.nodeLog = nodeLog; }

    public NodeLog getNodeLog() { return nodeLog; }
}
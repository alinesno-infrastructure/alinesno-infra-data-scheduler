package com.alinesno.infra.data.scheduler.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "node.log")
public class NodeLogProperties {

    private int queueSize = 20000;
    private int batchSize = 200;
    private long flushIntervalMs = 1000;
    private int maxRetries = 3;
    private long retryBackoffMs = 500;
    private String fallbackFile = "/tmp/node_logs_failures.log";
    private int workers = 1;
    private String collection = "node_logs";
    private int ttlDays = 7;

}
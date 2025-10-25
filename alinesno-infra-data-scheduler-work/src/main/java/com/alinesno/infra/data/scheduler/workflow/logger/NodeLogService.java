package com.alinesno.infra.data.scheduler.workflow.logger;

import com.alinesno.infra.data.scheduler.workflow.config.NodeLogProperties;
import com.alinesno.infra.data.scheduler.workflow.logger.event.NodeLogCreatedEvent;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 节点日志服务
 */
@Slf4j
@Service
public class NodeLogService {

    private final ApplicationEventPublisher eventPublisher;

    private final BlockingQueue<NodeLog> queue;
    private final ScheduledExecutorService scheduler;
    private final MongoNodeLogRepository repository;
    private final NodeLogProperties props;
    private final FallbackFileWriter fallback;
    private final AtomicLong dropped = new AtomicLong(0);

    /**
     * 原始密钥
     */
    @Setter
    @Getter
    protected Map<String, String> orgSecret;

    public NodeLogService(NodeLogProperties props, MongoNodeLogRepository repository , ApplicationEventPublisher eventPublisher) {
        this.props = props;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.queue = new ArrayBlockingQueue<>(props.getQueueSize());
        this.scheduler = Executors.newScheduledThreadPool(Math.max(1, props.getWorkers()),
                r -> { Thread t = new Thread(r, "node-log-writer"); t.setDaemon(true); return t; });
        this.fallback = new FallbackFileWriter(props.getFallbackFile());

        for (int i = 0; i < Math.max(1, props.getWorkers()); i++) {
            scheduler.scheduleWithFixedDelay(this::workerRun, 0, props.getFlushIntervalMs(), TimeUnit.MILLISECONDS);
        }
    }

    public boolean append(NodeLog nodeLog) {

        NodeLog sensitiveLog = NodeLog.maskSensitive(nodeLog , getOrgSecret()) ;
        log.debug("sensitiveLog: {}", sensitiveLog);

        boolean ok = queue.offer(sensitiveLog);
        if (!ok) {
            dropped.incrementAndGet();
            return false;
        }
        // publish event immediately (best-effort; event may be consumed before persistence)
        try {

            eventPublisher.publishEvent(new NodeLogCreatedEvent(sensitiveLog));
        } catch (Exception e) {
            // ignore publisher failures; do not fail business flow
        }
        return true;
    }

    private void workerRun() {
        List<NodeLog> batch = new ArrayList<>(props.getBatchSize());
        try {
            queue.drainTo(batch, props.getBatchSize());
            if (batch.isEmpty()) {
                NodeLog one = queue.poll(); // reduce latency for sparse writes
                if (one != null) batch.add(one);
            }
            if (batch.isEmpty()) return;
            boolean ok = tryInsertWithRetry(batch);
            if (!ok) {
                fallback.write(batch);
            }
        } catch (Throwable t) {
            // Unexpected error -> fallback
            fallback.write(batch);
        } finally {
            batch.clear();
        }
    }

    private boolean tryInsertWithRetry(List<NodeLog> batch) {
        int attempts = 0;
        while (attempts <= props.getMaxRetries()) {
            try {
                repository.insertBatch(batch);
                return true;
            } catch (Exception ex) {
                attempts++;
                try {
                    long backoff = props.getRetryBackoffMs() * attempts;
                    TimeUnit.MILLISECONDS.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return false;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        // final flush
        List<NodeLog> remaining = new ArrayList<>();
        queue.drainTo(remaining);
        if (!remaining.isEmpty()) {
            boolean ok = tryInsertWithRetry(remaining);
            if (!ok) fallback.write(remaining);
        }
    }

    // optional monitoring getters
    public long getDropped() { return dropped.get(); }
    public int getQueueSize() { return queue.size(); }
}
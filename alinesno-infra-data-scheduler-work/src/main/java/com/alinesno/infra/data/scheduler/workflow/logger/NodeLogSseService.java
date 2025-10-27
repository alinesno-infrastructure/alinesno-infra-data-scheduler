package com.alinesno.infra.data.scheduler.workflow.logger;

import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NodeLogSseService {

    // nodeId -> list of emitters
    private final ConcurrentMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // 超时：可以设置 0 = 永不超时（注意资源），或指定值（ms）
    private final long emitterTimeoutMs = 0L;

    public SseEmitter createEmitter(String nodeId, boolean sendInitialHeartbeat) {
        SseEmitter emitter = new SseEmitter(emitterTimeoutMs);
        emitters.computeIfAbsent(nodeId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(nodeId, emitter));
        emitter.onTimeout(() -> removeEmitter(nodeId, emitter));
        emitter.onError((e) -> removeEmitter(nodeId, emitter));

        if (sendInitialHeartbeat) {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("connected"));
            } catch (IOException ignored) {}
        }
        return emitter;
    }

    private void removeEmitter(String nodeId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(nodeId);
        if (list != null) {
            list.remove(emitter);
        }
    }

    public void publish(NodeLog nodeLog) {
        String nodeId = nodeLog.getNodeId();
        List<SseEmitter> list = emitters.get(nodeId);
        if (list == null || list.isEmpty()) return;

        String json;
        try {
            json = new ObjectMapper().writeValueAsString(nodeLog);
        } catch (JsonProcessingException e) {
            json = "{\"error\":\"serialize_failed\"}";
        }

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("log").data(json));
            } catch (IOException e) {
                removeEmitter(nodeId, emitter);
                try { emitter.complete(); } catch (Exception ignore) {}
            }
        }
    }

    // 可选：清理空 list 的定时任务等
}
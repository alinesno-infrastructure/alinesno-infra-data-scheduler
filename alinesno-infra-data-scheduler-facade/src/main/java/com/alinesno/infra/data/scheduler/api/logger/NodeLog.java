package com.alinesno.infra.data.scheduler.api.logger;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.lang.reflect.Array;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NodeLog 类用于记录节点日志信息。
 */
@NoArgsConstructor
@ToString
@Data
public class NodeLog {
    private String id;
    private String taskId;
    private String nodeId;
    private String nodeName;
    private String level;
    private String message;
    private Map<String,Object> meta;
    private Instant timestamp;

    public static NodeLog of(String taskId,
                             String nodeId,
                             String nodeName,
                             String level,
                             String message,
                             Map<String,Object> meta) {
        NodeLog n = new NodeLog();
        n.setTaskId(taskId);
        n.setNodeId(nodeId);
        n.setNodeName(nodeName);
        n.setLevel(level);
        n.setMessage(message);
        n.setMeta(meta);
        n.setTimestamp(Instant.now());
        return n;
    }


}
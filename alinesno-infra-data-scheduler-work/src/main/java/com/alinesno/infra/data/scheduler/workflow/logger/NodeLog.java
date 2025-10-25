package com.alinesno.infra.data.scheduler.workflow.logger;

import com.alinesno.infra.data.scheduler.workflow.utils.CommonsTextSecrets;
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

    /**
     * 返回脱敏后的 NodeLog（副本）。对 message 字段和 meta 中的字符串值进行脱敏处理，
     * 脱敏逻辑由 CommonsTextSecrets.maskSecretsPlaceholders 提供。
     */
    public static NodeLog maskSensitive(NodeLog src, Map<String, String> orgSecret) {
        if (src == null) return null;

        NodeLog n = new NodeLog();
        n.setId(src.getId());
        n.setTaskId(src.getTaskId());
        n.setNodeId(src.getNodeId());
        n.setNodeName(src.getNodeName());
        n.setLevel(src.getLevel());

        // 使用 CommonsTextSecrets 的脱敏方法处理 message
        n.setMessage(CommonsTextSecrets.maskSecretsPlaceholders(src.getMessage() , orgSecret));

        // 递归处理 meta 中的值
        n.setMeta(maskMetaMap(src.getMeta() , orgSecret));

        n.setTimestamp(src.getTimestamp());
        return n;
    }

    private static Map<String, Object> maskMetaMap(Map<String, Object> meta, Map<String, String> orgSecret) {
        if (meta == null) return null;
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : meta.entrySet()) {
            out.put(e.getKey(), maskValue(e.getValue() , orgSecret));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Object maskValue(Object value, Map<String, String> orgSecret) {
        if (value == null) return null;

        if (value instanceof String) {
            return CommonsTextSecrets.maskSecretsPlaceholders((String) value, orgSecret);
        }

        if (value instanceof Map) {
            Map<Object, Object> in = (Map<Object, Object>) value;
            Map<Object, Object> out = new LinkedHashMap<>();
            for (Map.Entry<Object, Object> e : in.entrySet()) {
                out.put(e.getKey(), maskValue(e.getValue(), orgSecret));
            }
            return out;
        }

        if (value instanceof Iterable) {
            List<Object> out = new ArrayList<>();
            for (Object item : (Iterable<?>) value) {
                out.add(maskValue(item, orgSecret));
            }
            return out;
        }

        // 处理数组（包括对象数组和基本类型数组）
        if (value.getClass().isArray()) {
            int len = Array.getLength(value);
            Object arr = Array.newInstance(value.getClass().getComponentType(), len);
            for (int i = 0; i < len; i++) {
                Object item = Array.get(value, i);
                Array.set(arr, i, maskValue(item, orgSecret));
            }
            return arr;
        }

        // 其他类型保持原样
        return value;
    }
}
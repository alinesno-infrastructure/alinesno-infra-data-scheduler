package com.alinesno.infra.data.scheduler.workflow.utils;

import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * NodeLog 相关的工具方法：序列化、按字节截断、时间格式化、构造返回体等。
 */
public final class NodeLogUtils {

    // 最大字节阈值（与原实现一致）
    public static final int MAX_BYTES = 1024_000;

    private NodeLogUtils() { /* no instantiation */ }

    public static class SerializedResult {
        public final List<String> lines;
        public final List<Integer> sizes;
        public final long totalBytes;

        public SerializedResult(List<String> lines, List<Integer> sizes, long totalBytes) {
            this.lines = lines;
            this.sizes = sizes;
            this.totalBytes = totalBytes;
        }
    }

    /**
     * 将 NodeLog 列表序列化为每行字符串（含时间前缀），并计算每行对应的字节长度（UTF-8，含换行符 1 字节）。
     * 返回：SerializedResult.lines, sizes, totalBytes
     */
    public static SerializedResult serializeLogs(List<NodeLog> logs, ObjectMapper objectMapper) {
        DateTimeFormatter zonedFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault());
        DateTimeFormatter localFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        List<String> serializedLines = new ArrayList<>(logs.size());
        List<Integer> sizes = new ArrayList<>(logs.size());
        long total = 0L;

        for (NodeLog ln : logs) {
            String json;
            try {
                json = objectMapper.writeValueAsString(ln);
            } catch (Exception e) {
                json = ln == null ? "" : ln.toString();
            }

            Object tsObj = ln == null ? null : ln.getTimestamp();
            String timeStr = formatTimestamp(tsObj, zonedFormatter, localFormatter);
            String line = "[" + timeStr + "] " + json;

            int byteLen = line.getBytes(StandardCharsets.UTF_8).length + 1; // 加上换行符的 1 字节
            serializedLines.add(line);
            sizes.add(byteLen);
            total += byteLen;
        }

        return new SerializedResult(serializedLines, sizes, total);
    }

    /**
     * 根据 byte 限制返回保留的日志行（按时间升序，即老->新）。
     * 若总量 <= maxBytes，返回原列表的浅拷贝。
     * 若没有任何一条能被包含（单条超限），至少返回最新一条（不截断内容）。
     */
    @NotNull
    public static List<String> truncateByBytes(List<String> serializedLines, List<Integer> sizes, int maxBytes) {
        if (serializedLines == null || serializedLines.isEmpty()) {
            return Collections.emptyList();
        }
        long total = sizes.stream().mapToLong(Integer::intValue).sum();
        if (total <= maxBytes) {
            return new ArrayList<>(serializedLines);
        }

        List<String> revKept = new ArrayList<>();
        long acc = 0L;
        for (int i = serializedLines.size() - 1; i >= 0; i--) {
            int sz = sizes.get(i);
            if (acc + sz > maxBytes) break;
            revKept.add(serializedLines.get(i));
            acc += sz;
        }
        if (revKept.isEmpty()) {
            // 单条超限，则至少返回最新一条
            revKept.add(serializedLines.get(serializedLines.size() - 1));
        }
        Collections.reverse(revKept); // 变回升序（老->新）
        return revKept;
    }

    /**
     * 将多行日志合并为单个文本片段，并根据 start 构造返回 map（log, hasMoreLog, nextOffset）。
     */
    @NotNull
    public static Map<String, Object> buildLogResponse(int start, List<String> keptLines) {
        StringBuilder sb = new StringBuilder();
        for (String line : keptLines) {
            sb.append(line).append('\n');
        }
        String full = sb.toString();
        int totalLen = full.length();

        int from = Math.max(0, start);
        String fragment = "";
        if (from < totalLen) {
            fragment = full.substring(from);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("log", fragment);
        data.put("hasMoreLog", false);
        data.put("nextOffset", totalLen);
        return data;
    }

    /**
     * 与原实现相同，支持多种 Timestamp 类型的格式化。
     */
    @NotNull
    public static String formatTimestamp(Object tsObj, DateTimeFormatter zonedFormatter, DateTimeFormatter localFormatter) {
        if (tsObj == null) return "";
        try {
            if (tsObj instanceof Instant) {
                return zonedFormatter.format((Instant) tsObj);
            } else if (tsObj instanceof OffsetDateTime) {
                return ((OffsetDateTime) tsObj).format(zonedFormatter);
            } else if (tsObj instanceof ZonedDateTime) {
                return ((ZonedDateTime) tsObj).format(zonedFormatter);
            } else if (tsObj instanceof LocalDateTime) {
                return ((LocalDateTime) tsObj).format(localFormatter);
            } else if (tsObj instanceof Date) {
                return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format((Date) tsObj);
            } else if (tsObj instanceof Long) {
                return zonedFormatter.format(Instant.ofEpochMilli((Long) tsObj));
            } else {
                return tsObj.toString();
            }
        } catch (Exception e) {
            return tsObj.toString();
        }
    }
}
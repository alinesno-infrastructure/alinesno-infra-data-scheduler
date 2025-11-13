package com.alinesno.infra.data.scheduler.workflow.controller;

import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.logger.MongoNodeLogQueryRepository;
import com.alinesno.infra.data.scheduler.workflow.logger.NodeLogSseService;
import com.alinesno.infra.data.scheduler.workflow.utils.NodeLogUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

/**
 * 节点日志控制类
 */
@Slf4j
@RestController
@RequestMapping("/api/infra/data/scheduler/nodeLogs")
public class NodeLogController {

    // 最大字节阈值：20KB
    private static final int MAX_BYTES = 1024_000;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MongoNodeLogQueryRepository queryRepository;
    private final NodeLogSseService sseService;

    @Autowired
    public NodeLogController(MongoNodeLogQueryRepository queryRepository,
                             NodeLogSseService sseService) {
        this.queryRepository = queryRepository;
        this.sseService = sseService;
    }

    // 历史查询：分页
    @GetMapping
    public Page<NodeLog> queryLogs(
            @RequestParam String nodeId,
            @RequestParam(required = false) String taskId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Long from,      // epoch ms
            @RequestParam(required = false) Long to,        // epoch ms
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Date fromDate = from == null ? null : new Date(from);
        Date toDate = to == null ? null : new Date(to);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return queryRepository.findByFilters(nodeId, taskId, level, fromDate, toDate, pageable);
    }

    // SSE 订阅：实时流（tail）
    @GetMapping(path = "/stream/{nodeId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNodeLogs(
            @PathVariable String nodeId,
            @RequestParam(required = false, defaultValue = "true") boolean heartbeat) {

        // 可选：验证当前用户是否有权限订阅该 nodeId
        SseEmitter emitter = sseService.createEmitter(nodeId, heartbeat);
        return emitter;
    }

    /**
     * 读取日志
     * @param processInstanceId
     * @param nodeId
     * @param start
     * @return
     */
    @GetMapping("/readLog")
    public R<Map<String , Object>> readLog(@RequestParam String processInstanceId,
                                           @RequestParam(required = false) String nodeId ,
                                           @RequestParam(required = false, defaultValue = "0") int start) {

        if(!StringUtils.hasLength(nodeId)){
            nodeId = null ;
        }

        try {
            // 1. 读取所有与 processInstanceId 相关的 NodeLog（按时间升序）
            Page<NodeLog> page = queryRepository.findByFilters(nodeId, processInstanceId, null, null, null, Pageable.unpaged());
            List<NodeLog> logs = new ArrayList<>(page.getContent());
            logs.sort(Comparator.comparing(NodeLog::getTimestamp, Comparator.nullsFirst(Comparator.naturalOrder())));

            // 2. 序列化并计算字节长度
            NodeLogUtils.SerializedResult serialized = NodeLogUtils.serializeLogs(logs, objectMapper);

            // 3. 按 MAX_BYTES 截断（保留最新的日志），工具方法返回升序（老->新）
            List<String> keptLines = NodeLogUtils.truncateByBytes(serialized.lines, serialized.sizes, NodeLogUtils.MAX_BYTES);

            // 4. 构造返回体
            Map<String, Object> data = NodeLogUtils.buildLogResponse(start, keptLines);
            return R.ok(data);
        } catch (Exception ex) {
            log.error("读取任务日志失败，processInstanceId={}", processInstanceId, ex);
            return R.fail("读取日志失败: " + ex.getMessage());
        }
    }
}
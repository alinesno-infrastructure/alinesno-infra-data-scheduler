package com.alinesno.infra.data.scheduler.spark.controller;

import com.alinesno.infra.data.scheduler.spark.model.SqlRequest;
import com.alinesno.infra.data.scheduler.spark.model.TaskMeta;
import com.alinesno.infra.data.scheduler.spark.model.TaskStatus;
import com.alinesno.infra.data.scheduler.spark.task.SparkSqlTaskManager;
import com.alinesno.infra.data.scheduler.spark.utils.SqlSecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Spark SQL 控制器
 */
@RestController
@RequestMapping("/api/spark-sql")
public class SparkSqlController {

    private final SparkSqlTaskManager taskManager;
    private final SqlSecurityService securityService;
    private final ScheduledExecutorService scheduler;

    // 控制器字段注入
    @Value("#{'${spark-sql.admin-users}'.split(',')}")
    private Set<String> adminUsers;

    @Autowired
    public SparkSqlController(SparkSqlTaskManager taskManager,
                              SqlSecurityService securityService,
                              @Qualifier("controllerScheduler") ScheduledExecutorService scheduler) {

        this.taskManager = taskManager;
        this.securityService = securityService;
        this.scheduler = scheduler;
    }

    @PostMapping(value = "/execute/raw")
    public DeferredResult<ResponseEntity<?>> executeRaw(@RequestBody String sql,
                                                        @RequestParam(defaultValue = "true") boolean async,
                                                        @RequestParam(defaultValue = "300000") long waitMs,
                                                        @RequestParam(required = false) String user) throws IOException {
        SqlRequest req = new SqlRequest();
        req.setSql(sql);
        req.setUser(user);
        req.setAsync(async);
        return execute(req, waitMs);
    }

    @PostMapping("/execute")
    public DeferredResult<ResponseEntity<?>> execute(@RequestBody SqlRequest req,
                                                     @RequestParam(defaultValue = "300000") long waitMs) throws IOException {
        if (req.getSql() == null || req.getSql().trim().isEmpty()) {
            DeferredResult<ResponseEntity<?>> dr = new DeferredResult<>();
            dr.setResult(ResponseEntity.badRequest().body(Collections.singletonMap("message", "SQL statement cannot be empty")));
            return dr;
        }

        String user = req.getUser() == null ? "anonymous" : req.getUser();

        // 1) 分割语句（安全分割）
        List<String> stmts;
        try {
            stmts = securityService.splitStatements(req.getSql());
            if (stmts.isEmpty()) {
                DeferredResult<ResponseEntity<?>> dr = new DeferredResult<>();
                dr.setResult(ResponseEntity.badRequest().body(Collections.singletonMap("message", "No valid SQL statements found")));
                return dr;
            }
        } catch (Exception ex) {
            DeferredResult<ResponseEntity<?>> dr = new DeferredResult<>();
            dr.setResult(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Failed to split SQL: " + ex.getMessage())));
            return dr;
        }

        // 2) 绑定参数（仅支持 ${name} 形式）
        List<String> boundStatements = new ArrayList<>();
        try {
            for (String s : stmts) {
                String bound = securityService.bindParams(s, req.getParams());
                boundStatements.add(bound);
            }
        } catch (Exception ex) {
            DeferredResult<ResponseEntity<?>> dr = new DeferredResult<>();
            dr.setResult(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "Parameter binding error: " + ex.getMessage())));
            return dr;
        }

        // 3) 语法解析与白名单校验
        for (String s : boundStatements) {
            boolean ok = securityService.isStatementAllowed(s, user, adminUsers);
            if (!ok) {
                DeferredResult<ResponseEntity<?>> dr = new DeferredResult<>();
                dr.setResult(ResponseEntity.status(HttpStatus.FORBIDDEN).body(Collections.singletonMap("message", "SQL statement not allowed or unsafe")));
                return dr;
            }
        }

        // 4) 提交任务
        String mergedSql = String.join(";\n", boundStatements);
        String taskId = taskManager.submit(mergedSql);

        if (req.isAsync()) {
            DeferredResult<ResponseEntity<?>> dr = new DeferredResult<>();
            Map<String, Object> resp = new HashMap<>();
            resp.put("taskId", taskId);
            resp.put("status", "PENDING");
            dr.setResult(ResponseEntity.accepted().body(resp));
            return dr;
        }

        // 同步等待：DeferredResult + scheduler 轮询
        DeferredResult<ResponseEntity<?>> dr = new DeferredResult<>(waitMs);
        dr.onTimeout(() -> {
            Map<String, Object> timeoutBody = new HashMap<>();
            timeoutBody.put("message", "timeout waiting for SQL result");
            timeoutBody.put("taskId", taskId);
            dr.setResult(ResponseEntity.status(504).body(timeoutBody));
        });

        final ScheduledFuture<?> poller = scheduler.scheduleAtFixedRate(() -> {
            try {
                Optional<TaskMeta> opt = taskManager.getMeta(taskId);
                if (!opt.isPresent()) {
                    dr.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Collections.singletonMap("message", "Task metadata missing for taskId: " + taskId)));
                    throw new CancellationException("stop poll");
                }
                TaskMeta meta = opt.get();
                if (meta.getStatus() != TaskStatus.RUNNING && meta.getStatus() != TaskStatus.PENDING) {
                    dr.setResult(ResponseEntity.ok(meta));
                    throw new CancellationException("done");
                }
            } catch (CancellationException ce) {
                throw ce;
            } catch (Exception ex) {
                dr.setResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", ex.getMessage())));
                throw new CancellationException("error and stop");
            }
        }, 0, 300, TimeUnit.MILLISECONDS);

        dr.onCompletion(() -> {
            if (!poller.isCancelled() && !poller.isDone()) {
                poller.cancel(true);
            }
        });

        return dr;
    }


    @GetMapping("/tasks/{id}")
    public ResponseEntity<?> getTask(@PathVariable String id) throws IOException {
        Optional<TaskMeta> opt = taskManager.getMeta(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(opt.get());
    }

    @GetMapping("/tasks/{id}/result")
    public ResponseEntity<Resource> downloadResult(@PathVariable String id) throws IOException {
        Optional<TaskMeta> opt = taskManager.getMeta(id);
        if (!opt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        TaskMeta meta = opt.get();
        if (meta.getResultPath() == null) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT);
        }
        Path p = Paths.get(meta.getResultPath());
        if (!Files.exists(p)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Resource r = new FileSystemResource(p.toFile());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + p.getFileName().toString() + "\"")
                .body(r);
    }
}
package com.alinesno.infra.data.scheduler.spark.controller;

import com.alinesno.infra.data.scheduler.spark.model.SqlRequest;
import com.alinesno.infra.data.scheduler.spark.model.TaskMeta;
import com.alinesno.infra.data.scheduler.spark.model.TaskStatus;
import com.alinesno.infra.data.scheduler.spark.task.TaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/spark-sql")
public class SparkSqlController {

    private final TaskManager taskManager;

    @Autowired
    public SparkSqlController(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    @PostMapping(value = "/execute/raw", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<?> executeRawPlain(@RequestBody String sql,
                                             @RequestParam(defaultValue = "true") boolean async) throws IOException {
        SqlRequest req = new SqlRequest();
        req.setSql(sql);
        return execute(req, async);
    }

    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestBody SqlRequest req, @RequestParam(defaultValue = "true") boolean async) throws IOException {
        if (req.getSql() == null || req.getSql().trim().isEmpty()) {
            Map<String, Object> body = new HashMap<String, Object>();
            body.put("success", false);
            body.put("message", "SQL statement cannot be empty");
            return ResponseEntity.badRequest().body(body);
        }

        String taskId = taskManager.submit(req.getSql());
        if (async) {
            Map<String, Object> resp = new HashMap<String, Object>();
            resp.put("taskId", taskId);
            resp.put("status", "PENDING");
            return ResponseEntity.accepted().body(resp);
        } else {
            long start = System.currentTimeMillis();
            long timeoutMs = 60_000; // configurable
            while (true) {
                Optional<TaskMeta> optMeta = taskManager.getMeta(taskId);
                if (!optMeta.isPresent()) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Task metadata missing for taskId: " + taskId);
                }
                TaskMeta meta = optMeta.get();
                if (meta.getStatus() != TaskStatus.RUNNING && meta.getStatus() != TaskStatus.PENDING) {
                    return ResponseEntity.ok(meta);
                }
                if (System.currentTimeMillis() - start > timeoutMs) {
                    Map<String, Object> timeoutBody = new HashMap<String, Object>();
                    timeoutBody.put("message", "timeout waiting for SQL result");
                    timeoutBody.put("taskId", taskId);
                    return ResponseEntity.status(504).body(timeoutBody);
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Thread interrupted while waiting for result");
                }
            }
        }
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
package com.alinesno.infra.data.scheduler.spark.controller;

import com.alinesno.infra.data.scheduler.spark.config.SparkProperties;
import com.alinesno.infra.data.scheduler.spark.model.PyRequest;
import com.alinesno.infra.data.scheduler.spark.model.TaskMeta;
import com.alinesno.infra.data.scheduler.spark.model.TaskStatus;
import com.alinesno.infra.data.scheduler.spark.task.SparkPyTaskManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 专门用于提交 PySpark 脚本的 Controller（与 SQL 控制器分离）
 * 路径：/api/spark-python
 */
@RestController
@RequestMapping("/api/spark-python")
public class SparkPythonController {

    private final SparkPyTaskManager taskManager;
    private final ScheduledExecutorService scheduler;
    private final SparkProperties sparkProperties;

    @Autowired
    public SparkPythonController(SparkPyTaskManager taskManager,
                                 SparkProperties sparkProperties,
                                 @Qualifier("controllerScheduler") ScheduledExecutorService scheduler) {
        this.taskManager = taskManager;
        this.sparkProperties = sparkProperties;
        this.scheduler = scheduler;
    }

    /**
     * 以 raw body 提交脚本（直接作为脚本内容）
     */
    @PostMapping(value = "/execute/raw")
    public DeferredResult<ResponseEntity<?>> executeRaw(@RequestBody String script,
                                                        @RequestParam(defaultValue = "true") boolean async,
                                                        @RequestParam(defaultValue = "300000") long waitMs,
                                                        @RequestParam(required = false) String user) throws IOException {
        PyRequest req = new PyRequest();
        req.setScript(script);
        req.setAsync(async);
        req.setUser(user);
        return execute(req, waitMs);
    }

    /**
     * JSON 请求体提交脚本（支持 script 或 scriptFile）
     */
    @PostMapping("/execute")
    public DeferredResult<ResponseEntity<?>> execute(@RequestBody PyRequest req,
                                                     @RequestParam(defaultValue = "300000") long waitMs) throws IOException {
        // 基本校验：必须提供 script 或 scriptFile
        if ((req.getScript() == null || req.getScript().trim().isEmpty())
                && (req.getScriptFile() == null || req.getScriptFile().trim().isEmpty())) {
            DeferredResult<ResponseEntity<?>> dr = new DeferredResult<>();
            dr.setResult(ResponseEntity.badRequest().body(Collections.singletonMap("message", "Script content or scriptFile URL must be provided")));
            return dr;
        }

        String user = req.getUser() == null ? "anonymous" : req.getUser();

        // 可选：基于 sparkProperties.getAdminUsers() 做权限校验
        // Set<String> admins = sparkProperties.getAdminUsers();
        // if (需要检查 && !admins.contains(user)) { ... }

        // TODO: 如需脚本安全检查，请在此处调用自定义 ScriptSecurityService

        // 脚本来源：优先使用脚本内容（script），否则传 scriptFile（URL 或路径）
        String payload;
        if (req.getScript() != null && !req.getScript().trim().isEmpty()) {
            payload = req.getScript();
        } else {
            payload = req.getScriptFile();
        }

        // 提交任务（SparkPyTaskManager.submit 接受 String payload）
        // 注意：SparkPyTaskManager 内部需要能识别 payload 是 script 内容还是 url（你可以约定
        // 如果 payload 以 http(s) 开头则当作远程文件，否则当作脚本文本）
        String taskId = taskManager.submit(payload);

        if (req.isAsync()) {
            DeferredResult<ResponseEntity<?>> dr = new DeferredResult<>();
            Map<String, Object> resp = new HashMap<>();
            resp.put("taskId", taskId);
            resp.put("status", "PENDING");
            dr.setResult(ResponseEntity.accepted().body(resp));
            return dr;
        }

        // 同步等待：轮询任务元信息
        DeferredResult<ResponseEntity<?>> dr = new DeferredResult<>(waitMs);
        dr.onTimeout(() -> {
            Map<String, Object> timeoutBody = new HashMap<>();
            timeoutBody.put("message", "timeout waiting for script result");
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

    /**
     * 查询任务元信息
     */
    @GetMapping("/tasks/{id}")
    public ResponseEntity<?> getTask(@PathVariable String id) throws IOException {
        Optional<TaskMeta> opt = taskManager.getMeta(id);
        if (!opt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(opt.get());
    }

    /**
     * 下载任务结果（支持文件或目录路径）
     */
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
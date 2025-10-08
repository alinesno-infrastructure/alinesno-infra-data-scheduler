package com.alinesno.infra.data.scheduler.adapter;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Spark Python API Consumer（基于 Hutool + Lombok）
 *
 * 使用示例：
 *   SparkPythonConsumer consumer = new SparkPythonConsumer(engine);
 *   // 异步提交脚本内容
 *   JSONObject resp = consumer.submitAsync("print('hello')", false, "alice");
 *   // 同步提交脚本文件（scriptFile 指向远程 URL 或本地内容标识）
 *   JSONObject resp2 = consumer.submitSync("http://example.com/myscript.py", true, "alice", 30000);
 *   JSONObject meta = consumer.getTaskMeta(taskId);
 *   consumer.downloadResult(taskId, new File("/tmp/result.zip"));
 */
@Slf4j
@RequiredArgsConstructor
public class SparkPythonConsumer {

    private final ComputeEngineEntity engine;

    // -----------------------
    // 对外方法（API）
    // -----------------------

    /**
     * 异步提交 PySpark 脚本（scriptOrFile 当 isScriptFile=true 时作为 scriptFile，否则作为 script 内容）
     *
     * @param scriptOrFile 脚本内容或脚本文件 URL/路径
     * @param isScriptFile 是否把第一个参数当作 scriptFile（true）否则当作 script 内容
     * @param user 发起用户（可为空）
     * @return 响应 JSON 对象
     */
    public JSONObject submitAsync(String scriptOrFile, boolean isScriptFile, String user) {
        PyRequestModel req = buildPyRequest(scriptOrFile, isScriptFile, user, true);
        return postExecute(req, null);
    }

    /**
     * 同步提交 PySpark 脚本并等待结果（waitMs 毫秒）
     *
     * @param scriptOrFile 脚本内容或脚本文件 URL/路径
     * @param isScriptFile 是否把第一个参数当作 scriptFile（true）否则当作 script 内容
     * @param user 发起用户
     * @param waitMs 等待超时时间，毫秒
     * @return 响应 JSON 对象
     */
    public JSONObject submitSync(String scriptOrFile, boolean isScriptFile, String user, long waitMs) {
        PyRequestModel req = buildPyRequest(scriptOrFile, isScriptFile, user, false);
        return postExecuteWithWaitMs(req, waitMs);
    }

    /**
     * 原始提交：将脚本作为 raw body 提交到 /execute/raw，
     * async/waitMs/user 通过 query 参数传递（与 provider 保持一致）
     *
     * @param script 脚本文本
     * @param async 是否异步
     * @param waitMs 同步等待时间（毫秒）
     * @param user 发起用户
     * @return 响应 JSON 对象
     */
    public JSONObject submitRaw(String script, boolean async, long waitMs, String user) {
        String url = buildUrl("/api/spark-python/execute/raw");
        url = appendQuery(url, "async", Boolean.toString(async));
        url = appendQuery(url, "waitMs", Long.toString(waitMs));
        if (user != null && !user.isEmpty()) {
            url = appendQuery(url, "user", user);
        }

        HttpRequest req = prepareRequest(HttpRequest.post(url))
                .body(script, "text/plain; charset=utf-8");

        HttpResponse resp = req.execute();
        return handleResponseToJson(resp);
    }

    /**
     * 获取任务元信息：GET /api/spark-python/tasks/{id}
     *
     * @param taskId 任务 ID
     * @return 解析后的 JSON 对象
     */
    public JSONObject getTaskMeta(String taskId) {
        String url = buildUrl("/api/spark-python/tasks/" + taskId);
        HttpResponse resp = prepareRequest(HttpRequest.get(url)).execute();
        return handleResponseToJson(resp);
    }

    /**
     * 下载任务结果：GET /api/spark-python/tasks/{id}/result
     *
     * @param taskId 任务 ID
     * @param destFile 目标文件
     */
    public void downloadResult(String taskId, File destFile) {
        String url = buildUrl("/api/spark-python/tasks/" + taskId + "/result");
        HttpResponse resp = prepareRequest(HttpRequest.get(url)).execute();

        int status = resp.getStatus();
        try {
            if (status == HttpStatus.HTTP_OK) {
                FileUtil.touch(destFile);
                try (InputStream in = resp.bodyStream()) {
                    IoUtil.copy(in, FileUtil.getOutputStream(destFile));
                }
            } else if (status == HttpStatus.HTTP_NO_CONTENT) {
                throw new RuntimeException("任务无结果可下载（204 NO CONTENT），taskId=" + taskId);
            } else if (status == HttpStatus.HTTP_NOT_FOUND) {
                throw new RuntimeException("结果文件未找到（404），taskId=" + taskId);
            } else {
                String body = resp.body();
                throw new RuntimeException("下载结果失败，status=" + status + ", body=" + body);
            }
        } catch (Exception e) {
            log.error("下载结果异常，taskId={}, msg={}", taskId, e.getMessage(), e);
            throw new RuntimeException("下载结果时发生异常: " + e.getMessage(), e);
        }
    }

    // -----------------------
    // 内部辅助方法
    // -----------------------

    private PyRequestModel buildPyRequest(String scriptOrFile, boolean isScriptFile, String user, boolean async) {
        PyRequestModel m = new PyRequestModel();
        if (isScriptFile) {
            m.setScriptFile(scriptOrFile);
            m.setScript(null);
        } else {
            m.setScript(scriptOrFile);
            m.setScriptFile(null);
        }
        m.setUser(user);
        m.setAsync(async);
        return m;
    }

    /**
     * 向 /api/spark-python/execute 发送 POST 请求（不带 waitMs）
     *
     * @param req 请求体对象
     * @param timeoutSeconds 覆盖超时时间（秒），可为 null（使用 engine.requestTimeout 或默认 30 秒）
     * @return 响应 JSON
     */
    private JSONObject postExecute(PyRequestModel req, Integer timeoutSeconds) {
        String url = buildUrl("/api/spark-python/execute");
        String jsonBody = JSONUtil.toJsonStr(req);

        HttpRequest httpReq = prepareRequest(HttpRequest.post(url))
                .body(jsonBody, "application/json; charset=utf-8");

        int timeoutMs = (int) TimeUnit.SECONDS.toMillis(resolveTimeoutSeconds(timeoutSeconds));
        httpReq.timeout(timeoutMs);

        HttpResponse resp = httpReq.execute();
        return handleResponseToJson(resp);
    }

    /**
     * 向 /api/spark-python/execute 发送 POST 并带 waitMs 参数（毫秒）
     *
     * @param req 请求体
     * @param waitMs 等待时长（毫秒）
     * @return 响应 JSON 对象
     */
    private JSONObject postExecuteWithWaitMs(PyRequestModel req, long waitMs) {
        String url = buildUrl("/api/spark-python/execute");
        url = appendQuery(url, "waitMs", Long.toString(waitMs));

        String jsonBody = JSONUtil.toJsonStr(req);
        HttpRequest httpReq = prepareRequest(HttpRequest.post(url))
                .body(jsonBody, "application/json; charset=utf-8");

        httpReq.timeout((int) waitMs);

        HttpResponse resp = httpReq.execute();
        return handleResponseToJson(resp);
    }

    /**
     * 准备 HttpRequest：设置超时、Authorization、Accept 等
     *
     * @param req Hutool HttpRequest
     * @return 准备后的 HttpRequest
     */
    private HttpRequest prepareRequest(HttpRequest req) {
        int timeoutMs = (int) TimeUnit.SECONDS.toMillis(resolveTimeoutSeconds(null));
        req.timeout(timeoutMs);

        String token = engine.getAdminUser();
        if (token != null && !token.trim().isEmpty()) {
            req.header("Authorization", "Bearer " + token);
        }
        req.header("Accept", "application/json, text/plain, */*");
        req.charset(StandardCharsets.UTF_8);
        return req;
    }

    /**
     * 根据响应构建 JSONObject（对非 2xx 抛异常）
     *
     * @param resp Hutool HttpResponse
     * @return JSONObject
     */
    private JSONObject handleResponseToJson(HttpResponse resp) {
        int status = resp.getStatus();
        String body = resp.body();

        if (status >= 200 && status < 300) {
            if (body == null || body.trim().isEmpty()) {
                return new JSONObject();
            }
            try {
                return JSONObject.parseObject(body);
            } catch (Exception ex) {
                JSONObject r = new JSONObject();
                r.put("message", body);
                return r;
            }
        } else {
            String msg = String.format("请求失败 status=%d, body=%s", status, body);
            log.warn(msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * 构建完整 URL（拼接 engineAddress 与 path）
     *
     * @param path 以 '/' 开头的 path
     * @return 完整 URL
     */
    private String buildUrl(String path) {
        String base = engine.getEngineAddress();
        Objects.requireNonNull(base, "engineAddress不能为空");
        base = base.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    /**
     * 给 URL 添加查询参数（简单拼接）
     */
    private String appendQuery(String url, String name, String value) {
        if (url.contains("?")) {
            return url + "&" + name + "=" + value;
        } else {
            return url + "?" + name + "=" + value;
        }
    }

    /**
     * 解析超时时间（秒）：优先使用传入参数，其次使用 engine.requestTimeout，最后使用 30 秒默认
     */
    private int resolveTimeoutSeconds(Integer override) {
        if (override != null) {
            return override;
        }
        Integer rt = engine.getRequestTimeout();
        if (rt != null && rt > 0) {
            return rt;
        }
        return 30;
    }

    // -----------------------
    // 内部 DTO：PyRequestModel（与 provider 的 PyRequest 对应）
    // -----------------------
    @Data
    @AllArgsConstructor
    private static class PyRequestModel {
        private String script;
        private String scriptFile;
        private String user;
        private boolean async = true;

        // 无参构造器用于 JSON 序列化场景
        public PyRequestModel() {}
    }
}
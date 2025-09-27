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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Spark SQL API Consumer（基于 Hutool + Lombok）
 *
 * 使用示例：
 *   SparkSqlConsumer consumer = new SparkSqlConsumer(engine);
 *   JSONObject resp = consumer.submitAsync("select 1", null, "alice");
 *   JSONObject meta = consumer.getTaskMeta(taskId);
 *   consumer.downloadResult(taskId, new File("/tmp/result.csv"));
 */
@Slf4j
@RequiredArgsConstructor
public class SparkSqlConsumer {

    // 直接注入 ComputeEngineEntity（由调用方传入）
    private final ComputeEngineEntity engine;

    // -----------------------
    // 对外方法（API）
    // -----------------------

    /**
     * 异步提交 SQL（控制端返回 taskId）
     *
     * @param sql SQL 文本
     * @param params 可选参数绑定
     * @param user 发起用户（可为空）
     * @return 响应 JSON 对象
     */
    public JSONObject submitAsync(String sql, Map<String, Object> params, String user) {
        SqlRequestModel req = new SqlRequestModel(sql, params, user, true);
        return postExecute(req, null);
    }

    /**
     * 同步提交 SQL 并等待结果（waitMs 单位毫秒）
     *
     * @param sql SQL 文本
     * @param params 可选参数绑定
     * @param user 发起用户
     * @param waitMs 等待超时时间，毫秒
     * @return 响应 JSON 对象
     */
    public JSONObject submitSync(String sql, Map<String, Object> params, String user, long waitMs) {
        SqlRequestModel req = new SqlRequestModel(sql, params, user, false);
        return postExecuteWithWaitMs(req, waitMs);
    }

    /**
     * 提交原始 SQL 到 /execute/raw
     *
     * @param sql SQL 文本
     * @param async 是否异步
     * @param waitMs 等待超时时间（毫秒）
     * @param user 发起用户
     * @return 响应 JSON 对象
     */
    public JSONObject submitRaw(String sql, boolean async, long waitMs, String user) {
        String url = buildUrl("/api/spark-sql/execute/raw");
        url = appendQuery(url, "async", Boolean.toString(async));
        url = appendQuery(url, "waitMs", Long.toString(waitMs));
        if (user != null && !user.isEmpty()) {
            url = appendQuery(url, "user", user);
        }

        HttpRequest req = prepareRequest(HttpRequest.post(url))
                .body(sql, "text/plain; charset=utf-8");

        HttpResponse resp = req.execute();
        return handleResponseToJson(resp);
    }

    /**
     * 获取任务元信息：GET /api/spark-sql/tasks/{id}
     *
     * @param taskId 任务 ID
     * @return 解析后的 JSON 对象
     */
    public JSONObject getTaskMeta(String taskId) {
        String url = buildUrl("/api/spark-sql/tasks/" + taskId);
        HttpResponse resp = prepareRequest(HttpRequest.get(url)).execute();
        return handleResponseToJson(resp);
    }

    /**
     * 下载任务结果：GET /api/spark-sql/tasks/{id}/result
     *
     * @param taskId 任务 ID
     * @param destFile 目标文件
     */
    public void downloadResult(String taskId, File destFile) {
        String url = buildUrl("/api/spark-sql/tasks/" + taskId + "/result");
        HttpResponse resp = prepareRequest(HttpRequest.get(url)).execute();

        int status = resp.getStatus();
        try {
            if (status == HttpStatus.HTTP_OK) {
                // 确保目标文件存在
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

    /**
     * 向 /api/spark-sql/execute 发送 POST 请求（不带 waitMs）
     *
     * @param req 请求体对象
     * @param timeoutSeconds 覆盖超时时间（秒），可为 null（使用 engine.requestTimeout 或默认 30 秒）
     * @return 响应 JSON
     */
    private JSONObject postExecute(SqlRequestModel req, Integer timeoutSeconds) {
        String url = buildUrl("/api/spark-sql/execute");
        String jsonBody = JSONUtil.toJsonStr(req);

        HttpRequest httpReq = prepareRequest(HttpRequest.post(url))
                .body(jsonBody, "application/json; charset=utf-8");

        int timeoutMs = (int) TimeUnit.SECONDS.toMillis(resolveTimeoutSeconds(timeoutSeconds));
        httpReq.timeout(timeoutMs);

        HttpResponse resp = httpReq.execute();
        return handleResponseToJson(resp);
    }

    /**
     * 向 /api/spark-sql/execute 发送 POST 并带 waitMs 参数（毫秒）
     *
     * @param req 请求体
     * @param waitMs 等待时长（毫秒）
     * @return 响应 JSON
     */
    private JSONObject postExecuteWithWaitMs(SqlRequestModel req, long waitMs) {
        String url = buildUrl("/api/spark-sql/execute");
        url = appendQuery(url, "waitMs", Long.toString(waitMs));

        String jsonBody = JSONUtil.toJsonStr(req);
        HttpRequest httpReq = prepareRequest(HttpRequest.post(url))
                .body(jsonBody, "application/json; charset=utf-8");

        int timeoutMs = (int) TimeUnit.SECONDS.toMillis(resolveTimeoutSeconds(null));
        httpReq.timeout(timeoutMs);

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
                // 如果返回不是标准对象，则把原始内容放到 message 字段中
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
    // 内部 DTO：SqlRequestModel（使用 Lombok 简化）
    // -----------------------
    @Data
    @AllArgsConstructor
    private static class SqlRequestModel {
        private String sql;
        private Map<String, Object> params;
        private String user;
        private boolean async = true;
    }
}
package com.alinesno.infra.data.scheduler.adapter.spark;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * SparkConsumer
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseSparkConsumer {

    public final ComputeEngineEntity engine;

    /**
     * 对应 controller 返回的 Map: { "applicationId": "...", "status": "SUBMITTED" }
     */
    @Data
    public static class SubmitRespDto implements Serializable {
        private String applicationId;
        private String status;
    }


    public String buildUrl(String path) {
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

    public String appendQuery(String url, String name, String value) {
        if (url.contains("?")) {
            return url + "&" + name + "=" + value;
        } else {
            return url + "?" + name + "=" + value;
        }
    }

    public int resolveTimeoutSeconds(Integer override) {
        if (override != null) {
            return override;
        }
        Integer rt = engine.getRequestTimeout();
        if (rt != null && rt > 0) {
            return rt;
        }
        return 30;
    }


    public HttpRequest prepareRequest(HttpRequest req) {
        String token = engine.getApiToken();
        if (token != null && !token.trim().isEmpty()) {
            req.header("X-Spark-Api-Token", token);
        } else {
            log.warn("API Token 为空（engine.getAdminUser()），请求可能被服务端拒绝");
        }
        req.header("Accept", "application/json, text/plain, */*");
        req.charset(StandardCharsets.UTF_8);
        return req;
    }

    public <T> R<T> handleResponseToR(HttpResponse resp, Class<T> dataClazz) {
        int status = resp.getStatus();
        String body = resp.body();

        // 空 body
        if (body == null || body.trim().isEmpty()) {
            if (status >= 200 && status < 300) {
                return R.ok(null); // 成功但无数据
            } else {
                return R.fail(status, "请求失败，HTTP status=" + status);
            }
        }

        // 2xx -> 尝试解析为 DTO（controller 返回的是 Map -> DTO 可直接解析）
        if (status >= 200 && status < 300) {
            try {
                T data = JSON.parseObject(body, dataClazz);
                // 如果解析后对象为 null 或字段为空，仍然返回 ok(null)
                if (data != null) {
                    return R.ok(data);
                } else {
                    // 若解析成 DTO 为 null，尝试解析 message/msg 字段
                    try {
                        JSONObject j = JSONObject.parseObject(body);
                        if (j.containsKey("message")) {
                            return R.ok(null, j.getString("message"));
                        }
                        if (j.containsKey("msg")) {
                            return R.ok(null, j.getString("msg"));
                        }
                    } catch (Exception ignore) { }
                    return R.ok(null);
                }
            } catch (Exception ex) {
                // 解析为 DTO 失败：尝试从 JSON 中提取 message/msg，或直接把原始 body 作为 msg 返回
                try {
                    JSONObject j = JSONObject.parseObject(body);
                    if (j.containsKey("message")) {
                        return R.ok(null, j.getString("message"));
                    }
                    if (j.containsKey("msg")) {
                        return R.ok(null, j.getString("msg"));
                    }
                } catch (Exception ignore) { }
                return R.ok(null, body);
            }
        } else {
            // 非 2xx：构造 fail，尝试从 JSON 中抽取 message/msg 字段
            String msg = body;
            try {
                JSONObject j = JSONObject.parseObject(body);
                if (j.containsKey("message")) {
                    msg = j.getString("message");
                } else if (j.containsKey("msg")) {
                    msg = j.getString("msg");
                }
            } catch (Exception ignore) { }
            return R.fail(status, msg);
        }
    }



}

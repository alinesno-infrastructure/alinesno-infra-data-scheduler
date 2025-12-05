package com.alinesno.infra.data.scheduler.adapter.spark;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Spark 任务查询客户端（适配 X-Spark-Api-Token 认证）
 */
@Slf4j
public class SparkTaskConsumer extends BaseSparkConsumer {

    public SparkTaskConsumer(ComputeEngineEntity engine) {
        super(engine);
    }

    public R<SparkTaskStatusDTO> queryTaskStatus(String appId) {
        String url = buildUrl("/api/spark-python/task/" + appId + "/status");
        HttpResponse resp = prepareRequest(HttpRequest.get(url)).execute();
        return handleResponse(resp, SparkTaskStatusDTO.class);
    }

    public R<LogUrlDTO> getLogUrl(String appId, int expireSeconds) {
        String url = buildUrl("/api/spark-python/task/" + appId + "/log/url");
        url = appendQuery(url, "expireSeconds", Integer.toString(expireSeconds));
        HttpResponse resp = prepareRequest(HttpRequest.get(url)).execute();
        return handleResponse(resp, LogUrlDTO.class);
    }

    public R<LogContentDTO> getLogContent(String appId, long offset, int length) {
        String url = buildUrl("/api/spark-python/task/" + appId + "/log/content");
        url = appendQuery(url, "offset", Long.toString(offset));
        url = appendQuery(url, "length", Integer.toString(length));
        HttpResponse resp = prepareRequest(HttpRequest.get(url)).execute();
        return handleResponse(resp, LogContentDTO.class);
    }

    public R<KillTaskDTO> killTask(String appId) {
        String url = buildUrl("/api/spark-python/task/" + appId + "/kill");
        HttpResponse resp = prepareRequest(HttpRequest.post(url)).execute();
        return handleResponse(resp, KillTaskDTO.class);
    }

    /**
     * 处理响应：解析为 R<T> 格式
     */
    private <T> R<T> handleResponse(HttpResponse resp, Class<T> clazz) {
        int status = resp.getStatus();
        String body = resp.body();

        if (status >= 200 && status < 300) {
            if (body == null || body.trim().isEmpty()) {
                return R.ok(null);
            }
            try {
                T data = JSON.parseObject(body, clazz);
                return R.ok(data);
            } catch (Exception ex) {
                log.warn("解析响应为 {} 失败，body={}, err={}", clazz.getSimpleName(), body, ex.getMessage());
                return R.fail("解析响应失败: " + ex.getMessage());
            }
        } else {
            String msg = String.format("请求失败 status=%d, body=%s", status, body);
            log.warn(msg);
            return R.fail(msg);
        }
    }

    // -----------------------
    // 响应包装类 和 DTO（保持不变）
    // -----------------------
    @Data
    public static class SparkTaskStatusDTO {
        private String appId;
        private String status;        // 例如 RUNNING, FINISHED, FAILED
        private String statusDesc;
        private String submitTime;
        private String finishTime;
        private Long durationMs;
        private Long logFileSize;
        private boolean hasMinioLog;
    }

    @Data
    public static class LogUrlDTO {
        private String appId;
        private String logUrl;
        private Integer expireSeconds;
        private String message;
    }

    @Data
    public static class LogContentDTO {
        private String appId;
        private Long offset;
        private Integer length;
        private String logContent;
    }

    @Data
    public static class KillTaskDTO {
        private String appId;
        private Boolean killed;
    }
}
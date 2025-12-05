package com.alinesno.infra.data.scheduler.adapter.spark;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 仅与提供的 SparkPythonController 对应的 Consumer 实现：
 * - /api/spark-python/execute （JSON body）
 * - /api/spark-python/execute/raw  （raw body + query async/waitMs/user）
 */
@Slf4j
public class SparkPythonConsumer extends BaseSparkConsumer {

    public SparkPythonConsumer(ComputeEngineEntity engine) {
        super(engine);
    }

    /**
     * 按 controller 定义：以 JSON 请求体提交脚本（使用 query 参数 async）
     *
     * 返回 R<SubmitRespDto>（code = HTTP status，data 若能解析为 SubmitRespDto 则填充）
     */
    public R<SubmitRespDto> submitAsync(String scriptOrFile, boolean isScriptFile, String apiToken) {
        String url = buildUrl("/api/spark-python/execute");
        // controller 的 execute 接口通过 query 参数接收 async
        url = appendQuery(url, "async", "true");

        PyRequestBody bodyObj = buildPyRequest(scriptOrFile, isScriptFile, apiToken);
        String jsonBody = JSONUtil.toJsonStr(bodyObj);

        HttpRequest req = prepareRequest(HttpRequest.post(url))
                .body(jsonBody, "application/json; charset=utf-8");

        HttpResponse resp = req.execute();
        return handleResponseToR(resp, SubmitRespDto.class);
    }

    /**
     * 对应 controller 的 /execute/raw ：将脚本作为 raw body 提交，
     * async/waitMs/user 通过 query 参数传递
     */
    public R<SubmitRespDto> submitRaw(String script, long waitMs) {
        String url = buildUrl("/api/spark-python/execute/raw");
        url = appendQuery(url, "waitMs", Long.toString(waitMs));
        url = appendQuery(url, "async", "false");

        log.debug("-->>> 请求链接:{}" , url);

        HttpRequest req = prepareRequest(HttpRequest.post(url))
                .body(script, "text/plain; charset=utf-8");

        HttpResponse resp = req.execute();
        return handleResponseToR(resp, SubmitRespDto.class);
    }

    // 简单请求体 DTO（对应 controller 的 PyRequest）
    @Data
    @AllArgsConstructor
    private static class PyRequestBody {
        private String script;
        private String scriptFile;
        private String user;

        public PyRequestBody() {}
    }

    private PyRequestBody buildPyRequest(String scriptOrFile, boolean isScriptFile, String apiToken) {
        PyRequestBody m = new PyRequestBody();
        if (isScriptFile) {
            m.setScriptFile(scriptOrFile);
            m.setScript(null);
        } else {
            m.setScript(scriptOrFile);
            m.setScriptFile(null);
        }
        m.setUser(apiToken);
        return m;
    }

}
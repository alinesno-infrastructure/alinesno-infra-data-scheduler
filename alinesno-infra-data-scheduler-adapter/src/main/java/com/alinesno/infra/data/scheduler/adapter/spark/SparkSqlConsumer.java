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
 * 仅与 SparkSqlController 对应的 Consumer：
 * - POST /api/spark-sql/execute  （JSON body, ?async）
 * - POST /api/spark-sql/execute/raw  （raw body + ?async&waitMs&user）
 *
 * 返回 R<SubmitRespDto>
 */
@Slf4j
public class SparkSqlConsumer extends BaseSparkConsumer {

    public SparkSqlConsumer(ComputeEngineEntity engine) {
        super(engine);
    }

    /**
     * JSON 提交（对应 controller 的 /execute），此方法设置 query async=true（异步提交并立即返回）
     */
    public R<SubmitRespDto> submitAsync(String sqlOrFile, boolean isSqlFile, String apiToke) {
        String url = buildUrl("/api/spark-sql/execute");
        url = appendQuery(url, "async", "true");

        SqlRequestModel body = buildSqlRequest(sqlOrFile, isSqlFile, apiToke);
        String jsonBody = JSONUtil.toJsonStr(body);

        HttpRequest req = prepareRequest(HttpRequest.post(url))
                .body(jsonBody, "application/json; charset=utf-8");

        HttpResponse resp = req.execute();
        return handleResponseToR(resp, SubmitRespDto.class);
    }

    /**
     * raw 提交（对应 controller 的 /execute/raw）
     */
    public R<SubmitRespDto> submitRaw(String sql, long waitMs) {
        String url = buildUrl("/api/spark-sql/execute/raw");
        url = appendQuery(url, "waitMs", Long.toString(waitMs));

        HttpRequest req = prepareRequest(HttpRequest.post(url))
                .body(sql, "text/plain; charset=utf-8");

        HttpResponse resp = req.execute();
        return handleResponseToR(resp, SubmitRespDto.class);
    }

    // 在 SparkSqlConsumer 类中加入以下方法
    private SqlRequestModel buildSqlRequest(String sqlOrFile, boolean isSqlFile, String apiToke) {
        SqlRequestModel m = new SqlRequestModel();
        if (isSqlFile) {
            m.setSqlFile(sqlOrFile);
            m.setSql(null);
        } else {
            m.setSql(sqlOrFile);
            m.setSqlFile(null);
        }
        m.setUser(apiToke);
        m.setAsync(true);
        return m;
    }

    // 简单请求体 DTO（对应 controller 的 SqlRequest）
    @Data
    @AllArgsConstructor
    private static class SqlRequestModel {
        private String sql;
        private String sqlFile;
        private String user;
        private boolean async = true;

        // 无参构造器（用于序列化/反序列化）
        public SqlRequestModel() {}
    }
}
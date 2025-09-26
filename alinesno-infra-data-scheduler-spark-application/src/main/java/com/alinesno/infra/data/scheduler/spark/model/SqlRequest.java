package com.alinesno.infra.data.scheduler.spark.model;

import lombok.Data;

import java.util.Map;

@Data
public class SqlRequest {
    private String sql;
    private Map<String, Object> params; // 可选，允许绑定参数
    private String user; // 发起用户
    private boolean async = true; // 默认异步
}

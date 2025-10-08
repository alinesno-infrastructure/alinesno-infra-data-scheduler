package com.alinesno.infra.data.scheduler.spark.model;

import lombok.Data;

import java.util.Map;

/**
 * PyRequest
 */
@Data
public class PyRequest {
    private String script;        // 脚本内容（优先）
    private String scriptFile;    // 脚本 URL 或集群可访问路径（可选）
    private Map<String, String> params; // 额外参数（保留）
    private boolean async = true;
    private String user;
}


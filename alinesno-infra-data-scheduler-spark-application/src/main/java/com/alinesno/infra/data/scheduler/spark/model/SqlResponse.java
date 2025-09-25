package com.alinesno.infra.data.scheduler.spark.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SqlResponse {
    private boolean success;
    private String message;
    private List<String> columns;
    private List<Map<String, Object>> data;
    private long rowCount;
    private long executionTimeMs;
}

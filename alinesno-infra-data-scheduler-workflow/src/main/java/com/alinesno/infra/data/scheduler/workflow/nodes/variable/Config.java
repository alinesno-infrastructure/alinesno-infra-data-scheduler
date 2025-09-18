package com.alinesno.infra.data.scheduler.workflow.nodes.variable;

import lombok.Data;

import java.util.List;

@Data
public class Config {
    // 字段列表
    private List<ConfigField> fields;
}
package com.alinesno.infra.data.scheduler.enums;

import lombok.Getter;

/**
 * 任务执行列表,包括shell/python/sql/maven/http/jar任务
 */
@Getter
public enum ExecutorTypeEnums {

    SHELL("shell", "shell"),
    JAR("jar", "jar"),
    PYTHON("python", "python"),
    SQL("sql", "sql"),
    MAVEN("maven", "maven"),
    HTTP("http", "http");

    private final String code;
    private final String desc;

    ExecutorTypeEnums(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}

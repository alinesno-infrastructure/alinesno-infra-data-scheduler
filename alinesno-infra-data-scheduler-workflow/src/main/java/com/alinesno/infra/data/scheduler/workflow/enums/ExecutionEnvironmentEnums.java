package com.alinesno.infra.data.scheduler.workflow.enums;

import lombok.Getter;

/**
 * 执行环境枚举类
 * 对应前端的Windows、macOS、Linux三个选项
 */
@Getter
public enum ExecutionEnvironmentEnums {
    // Windows系统
    WIN("win", "Windows"),
    // macOS系统
    MAC("mac", "macOS"),
    // Linux系统
    LINUX("linux", "Linux");

    // 对应前端的value值
    private final String code;
    // 显示名称
    private final String name;

    ExecutionEnvironmentEnums(String code, String name) {
        this.code = code;
        this.name = name;
    }

    // 根据编码获取枚举实例
    public static ExecutionEnvironmentEnums getByCode(String code) {
        for (ExecutionEnvironmentEnums environment : values()) {
            if (environment.code.equals(code)) {
                return environment;
            }
        }
        return null;
    }
}
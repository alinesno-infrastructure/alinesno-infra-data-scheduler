package com.alinesno.infra.data.scheduler.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 枚举类，用于表示告警状态
 *
 * 该枚举类提供了三种告警状态：等待执行（WAIT_EXECUTION），执行成功（EXECUTION_SUCCESS），执行失败（EXECUTION_FAILURE）
 * 使用该枚举可以方便地表示和处理不同的告警状态
 */
@Getter
public enum AlertStatusEnums {

    // 等待执行状态
    WAIT_EXECUTION(0, "waiting executed"),
    // 执行成功状态
    EXECUTION_SUCCESS(1, "execute successfully"),
    // 执行失败状态
    EXECUTION_FAILURE(2, "execute failed");

    /**
     * 构造函数，用于初始化枚举实例
     *
     * @param code 状态码，用于外部系统识别状态
     * @param descp 状态描述，提供状态的详细说明
     */
    AlertStatusEnums(int code, String descp){
        this.code = code;
        this.descp = descp;
    }

    /**
     * 状态码，用于数据库存储和外部系统识别
     */
    @EnumValue
    private final int code;

    /**
     * 状态描述，提供状态的详细说明
     */
    private final String descp;

}

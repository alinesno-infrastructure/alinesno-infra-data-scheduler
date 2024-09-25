package com.alinesno.infra.data.scheduler.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * @ToString： 自动生成toString方法，以便于对象的字符串表示。
 * @Data： 自动生成getter和setter方法，equals和hashCode方法，以及toString方法。
 * @JsonIgnoreProperties(ignoreUnknown = true)： 当JSON中有数据库表中没有的字段时，忽略未知字段。
 */
@ToString
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParamsDto {

    // 字段名与 JSON 键对应
    private String name;

    // 描述
    private String desc;

    // 注意布尔值在 JSON 中是 true/false，在 Java 中是 boolean
    private boolean delivery;

    // 重试次数，整型数值
    private int retryCount;

    // 环境变量
    private String env;

    // 原始脚本
    private String rawScript;

    // 资源名称
    private List<String> resourceId;

    // 自定义参数
    private Map<String,String> customParams;

}

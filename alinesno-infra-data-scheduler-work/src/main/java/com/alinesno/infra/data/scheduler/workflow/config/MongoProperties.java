package com.alinesno.infra.data.scheduler.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MongoDB 属性配置类
 */
@Data
@ConfigurationProperties(prefix = "spring.data.mongodb")
public class MongoProperties {

    private String uri;
    private String database;
    private String username;
    private String password;

}
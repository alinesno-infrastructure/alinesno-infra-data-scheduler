package com.alinesno.infra.data.scheduler.spark.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spark")
public class SparkProperties {
    
    private String master;
    private String appName;
    private Driver driver = new Driver();
    private Sql sql = new Sql();
    private Catalog catalog = new Catalog();
    private Hadoop hadoop = new Hadoop();
    
    // getter和setter方法
    @Data
    public static class Driver {
        private String bindAddress;
    }

    @Data
    public static class Sql {
        private String warehouseDir;
        private String defaultCatalog;
    }

    @Data
    public static class Catalog {
        private String className;
        private String warehouse;
        private String type;
        private String uri;
        private Jdbc jdbc = new Jdbc();
    }

    @Data
    public static class Jdbc {
        private boolean verifyServerCertificate;
        private boolean useSsl;
        private String user;
        private String password;
        private String driver;
    }

    @Data
    public static class Hadoop {
        private Oss oss = new Oss();
        // getter和setter
    }

    @Data
    public static class Oss {
        private String impl;
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
    }
}
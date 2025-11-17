package com.alinesno.infra.data.scheduler.spark.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Component
@ConfigurationProperties(prefix = "spark")
public class SparkProperties {

    private Set<String> adminUsers = new LinkedHashSet<>();

    private String sparkHome ;
    private String master;
    private String appName;
    private Driver driver = new Driver();
    private Sql sql = new Sql();
    private String deployMode = "client" ;
    private Oss oss = new Oss();

    private ExecutorJar executorJar = new ExecutorJar();

    private boolean uploadSqlToOss = false;
    private String ossBasePath = "spark-sqls/"; // 上传目录前缀

    /**
     * Executor 基本配置（建议通过 application.yml 调整）
     */
    private Executor executor = new Executor();

    /**
     * 动态资源分配配置
     */
    private DynamicAllocation dynamicAllocation = new DynamicAllocation();

    /**
     * 默认并行度（defaultParallelism）
     */
    private int defaultParallelism = 200;
    
    // getter和setter方法
    @Data
    public static class Driver {
        private String bindAddress;
    }

    @Data
    public static class Sql {
        private String warehouseDir;
        private String defaultCatalog;

        private int maxStatements = 100;
        private int maxSqlLength = 200_000;

        // Shuffle / Adaptive 参数
        private int shufflePartitions = 200;
        private boolean adaptiveEnabled = true;
        private String adaptiveTargetPostShuffleInputSize = "64m";
    }

    @Data
    public static class Oss {
        private String type;  // 云存储类型minio/aliyun
        private String impl;
        private String bucketName;
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
    }

    @Data
    public static class ExecutorJar {
        private String sparkSqlJobJar;
        private String sparkScalaJobJar;
    }

    @Data
    public static class Executor {
        private int instances = 5;
        private int cores = 1;
        private String memory = "8g";
        private String memoryOverhead = "2g";
    }

    @Data
    public static class DynamicAllocation {
        private boolean enabled = true;
        private int minExecutors = 2;
        private int maxExecutors = 20;
        private String executorIdleTimeout = "60s"; // e.g. "60s", "1m"
    }
}
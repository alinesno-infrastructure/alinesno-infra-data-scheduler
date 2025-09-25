package com.alinesno.infra.data.scheduler.spark.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {

    @Autowired
    private SparkProperties sparkProperties;

    @Bean
    public SparkSession sparkSession() {
        SparkSession.Builder builder = SparkSession.builder()
                .appName(sparkProperties.getAppName())
                .master(sparkProperties.getMaster())
                .config("spark.driver.bindAddress", sparkProperties.getDriver().getBindAddress())
                .config("spark.sql.warehouse.dir", sparkProperties.getSql().getWarehouseDir())
                .config("spark.sql.defaultCatalog", sparkProperties.getSql().getDefaultCatalog());

        // Iceberg Catalog配置
        SparkProperties.Catalog catalog = sparkProperties.getCatalog();
        builder.config("spark.sql.catalog.aip_catalog", catalog.getClassName())
                .config("spark.sql.catalog.aip_catalog.warehouse", catalog.getWarehouse())
                .config("spark.sql.catalog.aip_catalog.type", catalog.getType())
                .config("spark.sql.catalog.aip_catalog.uri", catalog.getUri())
                .config("spark.sql.catalog.aip_catalog.jdbc.verifyServerCertificate", String.valueOf(catalog.getJdbc().isVerifyServerCertificate()))
                .config("spark.sql.catalog.aip_catalog.jdbc.useSSL", String.valueOf(catalog.getJdbc().isUseSsl()))
                .config("spark.sql.catalog.aip_catalog.jdbc.user", catalog.getJdbc().getUser())
                .config("spark.sql.catalog.aip_catalog.jdbc.password", catalog.getJdbc().getPassword())
                .config("spark.sql.catalog.aip_catalog.jdbc.driver", catalog.getJdbc().getDriver());

        // OSS配置
        SparkProperties.Oss oss = sparkProperties.getHadoop().getOss();
        builder.config("spark.hadoop.fs.oss.impl", oss.getImpl())
                .config("spark.hadoop.fs.oss.endpoint", oss.getEndpoint())
                .config("spark.hadoop.fs.oss.accessKeyId", oss.getAccessKeyId())
                .config("spark.hadoop.fs.oss.accessKeySecret", oss.getAccessKeySecret());

        return builder.getOrCreate();
    }
}
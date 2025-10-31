package com.alinesno.infra.data.scheduler.spark.config;

import org.apache.spark.launcher.SparkLauncher;

public class SparkConfig {

    public static void sparkSession(SparkLauncher launcher , SparkProperties sparkProperties) {
        launcher.setConf("spark.executor.memory", sparkProperties.getExecutor().getMemory());
        launcher.setConf("spark.driver.memory", sparkProperties.getExecutor().getMemory());
        launcher.setConf("spark.driver.bindAddress", sparkProperties.getDriver().getBindAddress());
        launcher.setConf("spark.sql.warehouse.dir", sparkProperties.getSql().getWarehouseDir());
        launcher.setConf("spark.sql.defaultCatalog", sparkProperties.getSql().getDefaultCatalog());

//        // Iceberg Catalog配置
//        SparkProperties.Catalog catalog = sparkProperties.getCatalog();
//        launcher.setConf("spark.sql.catalog.aip_catalog", catalog.getClassName());
//        launcher.setConf("spark.sql.catalog.aip_catalog.warehouse", catalog.getWarehouse());
//        launcher.setConf("spark.sql.catalog.aip_catalog.type", catalog.getType());
//        launcher.setConf("spark.sql.catalog.aip_catalog.uri", catalog.getUri());
//        launcher.setConf("spark.sql.catalog.aip_catalog.jdbc.verifyServerCertificate", String.valueOf(catalog.getJdbc().isVerifyServerCertificate()));
//        launcher.setConf("spark.sql.catalog.aip_catalog.jdbc.useSSL", String.valueOf(catalog.getJdbc().isUseSsl()));
//        launcher.setConf("spark.sql.catalog.aip_catalog.jdbc.user", catalog.getJdbc().getUser());
//        launcher.setConf("spark.sql.catalog.aip_catalog.jdbc.password", catalog.getJdbc().getPassword());
//        launcher.setConf("spark.sql.catalog.aip_catalog.jdbc.driver", catalog.getJdbc().getDriver());
//
//        // OSS配置
//        SparkProperties.Oss oss = sparkProperties.getOss();
//        launcher.setConf("spark.hadoop.fs.oss.impl", oss.getImpl());
//        launcher.setConf("spark.hadoop.fs.oss.endpoint", oss.getEndpoint());
//        launcher.setConf("spark.hadoop.fs.oss.accessKeyId", oss.getAccessKeyId());
//        launcher.setConf("spark.hadoop.fs.oss.accessKeySecret", oss.getAccessKeySecret());

    }


}
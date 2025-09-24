package com.alinesno.infra.data.scheduler.spark.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {

    @Bean
    public SparkSession sparkSession() {
        return SparkSession.builder()
                .appName("Spring Boot Spark SQL API")
                .master("local[*]") // 本地模式运行，使用所有可用核心
                .config("spark.driver.bindAddress", "127.0.0.1")
                .config("spark.sql.warehouse.dir", "file:///tmp/spark-warehouse")
                .getOrCreate();
    }
}

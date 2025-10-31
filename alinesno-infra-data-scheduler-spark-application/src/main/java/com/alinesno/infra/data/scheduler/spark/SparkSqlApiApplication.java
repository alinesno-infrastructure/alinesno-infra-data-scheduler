package com.alinesno.infra.data.scheduler.spark;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SparkSqlApiApplication
 */
@EnableEncryptableProperties
@SpringBootApplication
public class SparkSqlApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SparkSqlApiApplication.class, args);
    }

}

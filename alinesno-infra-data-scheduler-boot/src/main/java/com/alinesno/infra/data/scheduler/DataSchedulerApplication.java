package com.alinesno.infra.data.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 集成一个Java开发示例工具
 */
@SpringBootApplication
@EnableScheduling
public class DataSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataSchedulerApplication.class, args);
	}

}
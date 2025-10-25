package com.alinesno.infra.data.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 调度任务执行器启动类
 */
@SpringBootApplication
public class SchedulerWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerWorkerApplication.class, args);
    }

}

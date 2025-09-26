package com.alinesno.infra.data.scheduler.spark.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExecutorConfig {

    // 长任务（Spark job）线程池：用于 TaskManager 提交 Spark 任务
    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(8);           // 根据并发 Spark 作业数调整
        exec.setMaxPoolSize(8);
        exec.setQueueCapacity(200);        // 有界队列，避免无限队列导致 OOM
        exec.setThreadNamePrefix("spark-task-");
        exec.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        exec.initialize();
        return exec;
    }

    // 控制/调度线程池：用于 Controller 的 DeferredResult 轮询（keep small）
    @Bean(name = "controllerScheduler")
    public ScheduledExecutorService controllerScheduler() {
        final AtomicInteger idx = new AtomicInteger(0);
        ThreadFactory tf = runnable -> {
            Thread t = new Thread(runnable);
            t.setName("ctrl-sched-" + idx.incrementAndGet());
            t.setDaemon(false);
            return t;
        };
        // 2 线程通常足够（轮询/超时处理），根据负载可适当增大到 4
        ScheduledThreadPoolExecutor sched = new ScheduledThreadPoolExecutor(2, tf);
        sched.setRemoveOnCancelPolicy(true);
        return sched;
    }
}
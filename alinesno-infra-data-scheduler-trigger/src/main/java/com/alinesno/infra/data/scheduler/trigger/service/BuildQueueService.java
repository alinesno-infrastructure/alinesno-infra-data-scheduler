package com.alinesno.infra.data.scheduler.trigger.service;

import com.alinesno.infra.data.scheduler.trigger.entity.JobEntity;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.concurrent.*;

@Service
public class BuildQueueService {
    private final BlockingQueue<JobEntity> queue = new LinkedBlockingQueue<>();
    private final ExecutorService workers = Executors.newFixedThreadPool(2);
    private final Future<?> consumerFuture;

    public BuildQueueService() {
        consumerFuture = workers.submit(this::consumerLoop);
    }

    public void enqueue(JobEntity job) {
        queue.offer(job);
    }

    private void consumerLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                JobEntity job = queue.take();
                runBuild(job);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void runBuild(JobEntity job) {
        System.out.println("Start build for " + job.getId() + " at " + Instant.now());
        try {
            Thread.sleep(3000);
            System.out.println("Finish build for " + job.getId() + " at " + Instant.now());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void shutdown() {
        consumerFuture.cancel(true);
        workers.shutdownNow();
    }
}
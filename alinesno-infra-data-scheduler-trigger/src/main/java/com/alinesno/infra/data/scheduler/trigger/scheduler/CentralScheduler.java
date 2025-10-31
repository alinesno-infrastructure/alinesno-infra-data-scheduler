package com.alinesno.infra.data.scheduler.trigger.scheduler;

import com.alinesno.infra.data.scheduler.trigger.entity.JobEntity;
import com.alinesno.infra.data.scheduler.trigger.entity.TriggerEntity;
import com.alinesno.infra.data.scheduler.trigger.resolver.CronResolverAdvanced;
import com.alinesno.infra.data.scheduler.trigger.service.BuildQueueService;
import com.alinesno.infra.data.scheduler.trigger.service.impl.JobServiceImpl;
import com.alinesno.infra.data.scheduler.trigger.service.impl.TriggerServiceImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CentralScheduler: 每分钟 tick 一次，读取 DB 中所有触发器并判断是否命中
 * 使用简单缓存：如果 trigger.cron 与 jobId 都没变，则复用解析后的 CronSchedule
 */
@Service
public class CentralScheduler {

    private final TriggerServiceImpl triggerService;
    private final JobServiceImpl jobService;
    private final BuildQueueService buildQueue;
    private final CronResolverAdvanced resolver = new CronResolverAdvanced();

    private static class CachedSchedule {
        String cronText;
        String jobId;
        CronResolverAdvanced.CronSchedule schedule;
    }

    private final ConcurrentHashMap<String, CachedSchedule> cache = new ConcurrentHashMap<>();

    public CentralScheduler(TriggerServiceImpl triggerService, JobServiceImpl jobService, BuildQueueService buildQueue) {
        this.triggerService = triggerService;
        this.jobService = jobService;
        this.buildQueue = buildQueue;
    }

    @Scheduled(cron = "0 * * * * *")
    public void tick() {
        LocalDateTime now = LocalDateTime.now();
        Collection<TriggerEntity> triggers = triggerService.list();
        for (TriggerEntity t : triggers) {
            try {
                String id = t.getId() + "";
                String cron = t.getCron();
                String jobId = t.getJobId() + "";

                CachedSchedule cs = cache.get(id);
                if (cs == null || !cron.equals(cs.cronText) || !Objects.equals(jobId, cs.jobId)) {
                    CronResolverAdvanced.CronSchedule schedule = resolver.parse(cron, jobId);
                    cs = new CachedSchedule();
                    cs.cronText = cron;
                    cs.jobId = jobId;
                    cs.schedule = schedule;
                    cache.put(id, cs);
                }

                if (cs.schedule.matches(now)) {
                    JobEntity job = jobService.getById(jobId);
                    if (job != null) {
                        buildQueue.enqueue(job);
                        System.out.println("Enqueued build for job " + job.getId() + " at " + now);
                    } else {
                        System.err.println("Job not found for trigger " + id + " jobId=" + jobId);
                    }
                }
            } catch (Exception ex) {
                System.err.println("Failed to evaluate trigger " + t.getId() + ": " + ex.getMessage());
            }
        }
    }
}
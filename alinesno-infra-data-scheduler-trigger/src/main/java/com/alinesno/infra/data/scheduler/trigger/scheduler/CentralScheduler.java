package com.alinesno.infra.data.scheduler.trigger.scheduler;

import com.alinesno.infra.data.scheduler.trigger.bean.BuildTask;
import com.alinesno.infra.data.scheduler.trigger.entity.JobEntity;
import com.alinesno.infra.data.scheduler.trigger.entity.TriggerEntity;
import com.alinesno.infra.data.scheduler.trigger.resolver.CronResolverAdvanced;
import com.alinesno.infra.data.scheduler.trigger.service.BuildQueueService;
import com.alinesno.infra.data.scheduler.trigger.service.impl.JobServiceImpl;
import com.alinesno.infra.data.scheduler.trigger.service.impl.TriggerServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CentralScheduler {

    private final TriggerServiceImpl triggerService;
    private final JobServiceImpl jobService;
    private final BuildQueueService buildQueue;
    private final RedissonClient redissonClient;
    private final CronResolverAdvanced resolver = new CronResolverAdvanced();

    private static class CachedSchedule {
        String cronText;
        String jobId;
        CronResolverAdvanced.CronSchedule schedule;
    }

    // 触发器解析缓存：key = triggerId
    private final ConcurrentHashMap<String, CachedSchedule> cache = new ConcurrentHashMap<>();

    @Value("${central.scheduler.lock.name:central-scheduler-lock}")
    private String lockName;

    // 锁自动释放时间（秒），应设置为预计最大执行时间 + 余量
    @Value("${central.scheduler.lock.lease-seconds:240}")
    private long lockLeaseSeconds;

    public CentralScheduler(TriggerServiceImpl triggerService,
                            JobServiceImpl jobService,
                            BuildQueueService buildQueue,
                            RedissonClient redissonClient) {
        this.triggerService = triggerService;
        this.jobService = jobService;
        this.buildQueue = buildQueue;
        this.redissonClient = redissonClient;
    }

    /**
     * 每次执行完成后再等待 central.scheduler.delay 毫秒再执行下一次（fixedDelay）。
     * 使用 Redisson 分布式锁，确保在分布式部署中同一时刻只有一个实例执行 tick，从而避免重复触发。
     */
    @Scheduled(cron = "0 * * * * *")
    public void tick() {
        long start = System.currentTimeMillis();
        RLock lock = redissonClient.getLock(lockName);
        boolean locked = false;
        try {
            // 不等待锁（立即返回）；如需等待一段时间再尝试，可将第一个参数改为 waitTime（秒）
            locked = lock.tryLock(0, lockLeaseSeconds, TimeUnit.SECONDS);
            if (!locked) {
                log.debug("已有实例持有中央调度器锁，跳过本次执行");
                return;
            }

            // 执行实际调度逻辑
            doTickLogic();

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("CentralScheduler：tick 被中断", ie);
        } catch (Exception ex) {
            log.error("CentralScheduler：执行出错", ex);
        } finally {
            try {
                if (locked && lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (Exception unlockEx) {
                log.warn("CentralScheduler：释放锁失败", unlockEx);
            }
            long elapsed = System.currentTimeMillis() - start;
            log.debug("CentralScheduler：本次执行完成，耗时 {} ms", elapsed);
        }
    }

    private void doTickLogic() {
        LocalDateTime now = LocalDateTime.now();
        Collection<TriggerEntity> triggers = triggerService.list();
        for (TriggerEntity t : triggers) {
            try {
                String id = String.valueOf(t.getId());
                String cron = t.getCron();
                String jobId = String.valueOf(t.getJobId());

                CachedSchedule cs = cache.get(id);
                if (cs == null || !Objects.equals(cron, cs.cronText) || !Objects.equals(jobId, cs.jobId)) {
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
                        BuildTask task = buildQueue.enqueue(job, t.getId());
                        log.debug("触发器 {} 命中，触发时间：{} , 任务:{}", id, now , task);
                    } else {
                        log.debug("任务 {} 未找到，触发器 {}", jobId, id);
                    }
                }else{
                    log.debug("触发器 {} 未命中，触发时间：{}", id, now);
                }
            } catch (Exception ex) {
                log.debug("处理触发器时出错，id: " + t.getId(), ex);
            }
        }
    }

    // 清除指定触发器的缓存（触发器更新或删除时调用）
    public void invalidateCache(String triggerId) {
        if (triggerId == null) return;
        cache.remove(triggerId);
    }

    // 根据 jobId 清除相关触发器缓存（当 Job 修改时调用）
    public void invalidateCacheByJobId(Long jobId) {
        if (jobId == null) return;
        String jobIdStr = String.valueOf(jobId);
        cache.entrySet().removeIf(e -> jobIdStr.equals(e.getValue().jobId));
    }

    // 清除全部缓存（慎用）
    public void invalidateAllCache() {
        cache.clear();
    }
}
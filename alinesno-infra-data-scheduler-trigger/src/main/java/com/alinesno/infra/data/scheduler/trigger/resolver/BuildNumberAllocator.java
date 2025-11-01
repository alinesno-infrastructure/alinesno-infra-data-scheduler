package com.alinesno.infra.data.scheduler.trigger.resolver;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

/**
 * 构建编号分配器
 */
@Component
public class BuildNumberAllocator {
    private final RedissonClient redissonClient;

    public BuildNumberAllocator(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public int nextBuildNumber(Long jobId) {
        String key = "job:build:seq:" + jobId;
        RAtomicLong seq = redissonClient.getAtomicLong(key);
        long v = seq.incrementAndGet();
        // 防护：确保不超过 Integer.MAX_VALUE
        if (v > Integer.MAX_VALUE) {
            seq.set(1);
            return 1;
        }
        return (int) v;
    }
}
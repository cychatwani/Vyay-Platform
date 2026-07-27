package com.vyay.core.services;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ApiCounterService {

    private final StringRedisTemplate redisTemplate;

    public ApiCounterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Long incrementGlobalCount() {
        // "api:count" is the Redis key
        return redisTemplate.opsForValue().increment("api:count");
    }

    public Long incrementPerMinuteCount() {
        String key = "api:count:" + (System.currentTimeMillis() / 60000); // minute bucket
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, java.time.Duration.ofMinutes(2)); // auto-expire
        return count;
    }

    public Long getGlobalCount() {
        String value = redisTemplate.opsForValue().get("api:count");
        return value != null ? Long.parseLong(value) : 0L;
    }
}

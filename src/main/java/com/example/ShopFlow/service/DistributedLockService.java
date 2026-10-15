package com.example.ShopFlow.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;


@Service
public class DistributedLockService {

    private final StringRedisTemplate redisTemplate;

    private static final String RELEASE_LOCK_LUA_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else " +
            "return 0 " +
            "end ";

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(RELEASE_LOCK_LUA_SCRIPT, Long.class);

    public DistributedLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean acquireLock(String lockKey, String token, Long expireSeconds){
        Boolean sucess = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, token, Duration.ofSeconds(expireSeconds));
        return Boolean.TRUE.equals(sucess);
    }

    public  boolean releaseLock(String lockKey, String token){
        Long result = redisTemplate.execute(RELEASE_SCRIPT,  List.of(lockKey), token);
        return Long.valueOf(1L).equals(result);
    }
}

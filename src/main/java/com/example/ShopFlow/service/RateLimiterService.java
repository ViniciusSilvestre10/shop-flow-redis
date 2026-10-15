package com.example.ShopFlow.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, long limit, long windowInSeconds){

        Long currentRequests = redisTemplate.opsForValue().increment(key);

        if (currentRequests != null && currentRequests==1){
            redisTemplate.expire(key, Duration.ofSeconds(windowInSeconds));
        }

        return currentRequests != null && currentRequests <= limit;
    }

    public long getTtlInSeconds(String key){
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return (expire !=  null && expire>0) ? expire : 0;
    }

}

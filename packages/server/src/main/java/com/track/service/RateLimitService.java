package com.track.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 限流服务
 * 基于 Redis 实现 IP 和 AppId 限流
 */
@Service
public class RateLimitService {
    
    private static final int IP_LIMIT = 1000; // 每分钟最多1000次
    private static final int APP_LIMIT = 5000; // 每个AppId每分钟最多5000次
    private static final int TIME_WINDOW_SECONDS = 60; // 时间窗口60秒
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public RateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * IP 限流
     * @param ip IP 地址
     * @return true 如果允许，false 如果超过限制
     */
    public boolean isAllowed(String ip) {
        String key = "rate:ip:" + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == 1) {
            // 第一次请求，设置过期时间
            redisTemplate.expire(key, TIME_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        
        return count <= IP_LIMIT;
    }
    
    /**
     * AppId + IP 限流
     * @param appId 应用 ID
     * @param ip IP 地址
     * @return true 如果允许，false 如果超过限制
     */
    public boolean isAllowed(String appId, String ip) {
        // 先检查 IP 限流
        if (!isAllowed(ip)) {
            return false;
        }
        
        // 再检查 AppId 限流
        String appKey = "rate:app:" + appId;
        Long count = redisTemplate.opsForValue().increment(appKey);
        
        if (count == 1) {
            // 第一次请求，设置过期时间
            redisTemplate.expire(appKey, TIME_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        
        return count <= APP_LIMIT;
    }
}


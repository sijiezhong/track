package com.track.service;

import com.track.dto.SessionInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Session 服务
 * 管理 Redis 中的 Session 数据
 */
@Service
public class SessionService {
    
    private static final String SESSION_PREFIX = "track:session:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public SessionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 保存 Session 到 Redis
     * @param sessionId Session ID
     * @param appId 应用 ID
     * @param userId 用户 ID
     * @param userProps 用户属性
     * @param ttlMinutes TTL（分钟），0 表示不过期
     */
    public void saveSession(String sessionId, String appId, String userId, 
                           Map<String, Object> userProps, int ttlMinutes) {
        SessionInfo sessionInfo = new SessionInfo(appId, userId, userProps, ttlMinutes);
        String key = SESSION_PREFIX + sessionId;
        
        if (ttlMinutes == 0) {
            // 不过期
            redisTemplate.opsForValue().set(key, sessionInfo);
        } else {
            redisTemplate.opsForValue().set(key, sessionInfo, 
                ttlMinutes, TimeUnit.MINUTES);
        }
    }
    
    /**
     * 获取 Session 信息
     * @param sessionId Session ID
     * @return SessionInfo 或 null（如果不存在）
     */
    public SessionInfo getSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        return (SessionInfo) redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 刷新 Session TTL
     * @param sessionId Session ID
     * @param ttlMinutes TTL（分钟），0 表示不过期
     */
    public void refreshSession(String sessionId, int ttlMinutes) {
        SessionInfo sessionInfo = getSession(sessionId);
        if (sessionInfo == null) {
            return;
        }
        
        String key = SESSION_PREFIX + sessionId;
        if (ttlMinutes == 0) {
            // 设置一个很长的过期时间（近似永久）
            redisTemplate.expire(key, Duration.ofDays(365 * 100));
        } else {
            redisTemplate.expire(key, ttlMinutes, TimeUnit.MINUTES);
        }
    }
    
    /**
     * 删除 Session
     * @param sessionId Session ID
     */
    public void deleteSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        redisTemplate.delete(key);
    }
}


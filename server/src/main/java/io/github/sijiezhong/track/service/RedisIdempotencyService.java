package io.github.sijiezhong.track.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.sijiezhong.track.config.IdempotencyProperties;
import io.github.sijiezhong.track.dto.IdempotentSummary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 基于 Redis 的幂等服务实现
 * 仅在 StringRedisTemplate bean 存在时创建（即 Redis 可用时）
 */
@Service
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisIdempotencyService implements IdempotencyService {

    private static final String PREFIX = "idem:";

    private final StringRedisTemplate redis;
    private final IdempotencyProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public RedisIdempotencyService(StringRedisTemplate redis, IdempotencyProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    @Override
    public boolean checkAndSet(String key, IdempotentSummary summary) {
        if (key == null || key.isEmpty()) return true; // 无键则视为不启用幂等
        String redisKey = PREFIX + key;
        try {
            String val = objectMapper.writeValueAsString(summary);
            Duration ttl = Duration.ofSeconds(properties.getTtlSeconds());
            Boolean set = redis.opsForValue().setIfAbsent(redisKey, val, ttl);
            return Boolean.TRUE.equals(set);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("幂等摘要序列化失败", e);
        }
    }

    @Override
    public Optional<IdempotentSummary> findSummary(String key) {
        if (key == null || key.isEmpty()) return Optional.empty();
        String redisKey = PREFIX + key;
        String val = redis.opsForValue().get(redisKey);
        if (val == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(val, IdempotentSummary.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

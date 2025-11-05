package com.track.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Redis 配置测试
 * 验证 RedisTemplate 配置正确性
 */
class RedisConfigTest {
    
    @Test
    void testRedisTemplateBean() {
        RedisConfig redisConfig = new RedisConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        
        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);
        
        assertNotNull(template);
        assertEquals(connectionFactory, template.getConnectionFactory());
    }
    
    @Test
    void testRedisTemplateSerialization() {
        RedisConfig redisConfig = new RedisConfig();
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        
        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);
        
        // 验证序列化器已配置
        assertNotNull(template.getKeySerializer());
        assertNotNull(template.getValueSerializer());
        assertNotNull(template.getHashKeySerializer());
        assertNotNull(template.getHashValueSerializer());
    }
}


package com.track.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RateLimitService 单元测试
 * 验证限流功能的正确性
 */
@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {
    
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;
    
    @InjectMocks
    private RateLimitService rateLimitService;
    
    private static final String IP = "192.168.1.1";
    private static final String APP_ID = "test-app";
    
    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    void testIsAllowedByIPWithinLimit() {
        // Given - IP 请求次数在限制内
        when(valueOperations.increment("rate:ip:" + IP)).thenReturn(500L);
        
        // When
        boolean result = rateLimitService.isAllowed(IP);
        
        // Then
        assertTrue(result);
        // 非首次请求不应设置过期时间
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }
    
    @Test
    void testIsAllowedByIPExceedsLimit() {
        // Given - IP 请求次数超过限制（1000次/分钟）
        when(valueOperations.increment("rate:ip:" + IP)).thenReturn(1001L);
        
        // When
        boolean result = rateLimitService.isAllowed(IP);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void testIsAllowedByIPFirstRequest() {
        // Given - 第一次请求
        when(valueOperations.increment("rate:ip:" + IP)).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        // When
        boolean result = rateLimitService.isAllowed(IP);
        
        // Then
        assertTrue(result);
        verify(redisTemplate).expire(eq("rate:ip:" + IP), eq(60L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    void testIsAllowedByAppIdAndIPWithinLimit() {
        // Given - AppId 和 IP 请求次数都在限制内
        when(valueOperations.increment("rate:ip:" + IP)).thenReturn(500L);
        when(valueOperations.increment("rate:app:" + APP_ID)).thenReturn(2000L);
        
        // When
        boolean result = rateLimitService.isAllowed(APP_ID, IP);
        
        // Then
        assertTrue(result);
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }
    
    @Test
    void testIsAllowedByAppIdAndIPExceedsIPLimit() {
        // Given - IP 限制超过，AppId 限制未超过
        when(valueOperations.increment("rate:ip:" + IP)).thenReturn(1001L);
        
        // When
        boolean result = rateLimitService.isAllowed(APP_ID, IP);
        
        // Then
        assertFalse(result);
        // IP 限制超过时，不应该检查 AppId
        verify(valueOperations, never()).increment("rate:app:" + APP_ID);
    }
    
    @Test
    void testIsAllowedByAppIdAndIPExceedsAppLimit() {
        // Given - IP 限制未超过，AppId 限制超过（5000次/分钟）
        when(valueOperations.increment("rate:ip:" + IP)).thenReturn(500L);
        when(valueOperations.increment("rate:app:" + APP_ID)).thenReturn(5001L);
        
        // When
        boolean result = rateLimitService.isAllowed(APP_ID, IP);
        
        // Then
        assertFalse(result);
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }
    
    @Test
    void testIsAllowedByAppIdFirstRequest() {
        // Given - AppId 第一次请求
        when(valueOperations.increment("rate:ip:" + IP)).thenReturn(1L);
        when(valueOperations.increment("rate:app:" + APP_ID)).thenReturn(1L);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        // When
        boolean result = rateLimitService.isAllowed(APP_ID, IP);
        
        // Then
        assertTrue(result);
        verify(redisTemplate).expire(eq("rate:app:" + APP_ID), eq(60L), eq(TimeUnit.SECONDS));
    }
    
    @Test
    void testRateLimitCounterAccuracy() {
        // Given - 验证计数准确性
        when(valueOperations.increment("rate:ip:" + IP)).thenReturn(999L);
        
        // When
        boolean result1 = rateLimitService.isAllowed(IP);
        
        // Then
        assertTrue(result1);
        
        // Given - 第1000次请求
        when(valueOperations.increment("rate:ip:" + IP)).thenReturn(1000L);
        
        // When
        boolean result2 = rateLimitService.isAllowed(IP);
        
        // Then
        assertTrue(result2); // 刚好1000次，应该允许
        
        // Given - 第1001次请求
        when(valueOperations.increment("rate:ip:" + IP)).thenReturn(1001L);
        
        // When
        boolean result3 = rateLimitService.isAllowed(IP);
        
        // Then
        assertFalse(result3); // 超过1000次，应该拒绝
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }
}


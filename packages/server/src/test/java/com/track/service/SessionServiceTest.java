package com.track.service;

import com.track.dto.SessionInfo;
import com.track.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SessionService 单元测试
 * 验证 Session 的创建、获取、刷新、删除功能
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {
    
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    
    @Mock
    private ValueOperations<String, Object> valueOperations;
    
    @Mock
    private ProjectRepository projectRepository;
    
    @InjectMocks
    private SessionService sessionService;
    
    private static final String SESSION_ID = "test-session-id";
    private static final String APP_ID = "test-app";
    private static final String USER_ID = "user-123";
    private static final int TTL_MINUTES = 1440;
    
    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }
    
    @Test
    void testSaveSession() {
        // Given
        Map<String, Object> userProps = new HashMap<>();
        userProps.put("plan", "premium");
        
        // When
        sessionService.saveSession(SESSION_ID, APP_ID, USER_ID, userProps, TTL_MINUTES);
        
        // Then
        verify(valueOperations).set(eq("track:session:" + SESSION_ID), any(SessionInfo.class), 
            eq((long) TTL_MINUTES), eq(TimeUnit.MINUTES));
    }
    
    @Test
    void testSaveSessionWithZeroTTL() {
        // Given
        Map<String, Object> userProps = new HashMap<>();
        
        // When
        sessionService.saveSession(SESSION_ID, APP_ID, USER_ID, userProps, 0);
        
        // Then - 0 表示不过期，应该调用 set 而不是 set with TTL
        verify(valueOperations).set(eq("track:session:" + SESSION_ID), any(SessionInfo.class));
    }
    
    @Test
    void testGetSession() {
        // Given
        Map<String, Object> userProps = new HashMap<>();
        userProps.put("plan", "premium");
        SessionInfo sessionInfo = new SessionInfo(APP_ID, USER_ID, userProps, TTL_MINUTES);
        when(valueOperations.get("track:session:" + SESSION_ID)).thenReturn(sessionInfo);
        
        // When
        SessionInfo result = sessionService.getSession(SESSION_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(APP_ID, result.getAppId());
        assertEquals(USER_ID, result.getUserId());
        assertEquals(userProps, result.getUserProps());
        assertEquals(TTL_MINUTES, result.getTtlMinutes());
    }
    
    @Test
    void testGetSessionNotFound() {
        // Given
        when(valueOperations.get("track:session:" + SESSION_ID)).thenReturn(null);
        
        // When
        SessionInfo result = sessionService.getSession(SESSION_ID);
        
        // Then
        assertNull(result);
    }
    
    @Test
    void testRefreshSession() {
        // Given
        Map<String, Object> userProps = new HashMap<>();
        SessionInfo sessionInfo = new SessionInfo(APP_ID, USER_ID, userProps, TTL_MINUTES);
        when(valueOperations.get("track:session:" + SESSION_ID)).thenReturn(sessionInfo);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        
        // When
        sessionService.refreshSession(SESSION_ID, TTL_MINUTES);
        
        // Then
        verify(redisTemplate).expire(eq("track:session:" + SESSION_ID), 
            eq((long) TTL_MINUTES), eq(TimeUnit.MINUTES));
    }
    
    @Test
    void testRefreshSessionNotFound() {
        // Given
        when(valueOperations.get("track:session:" + SESSION_ID)).thenReturn(null);
        
        // When
        sessionService.refreshSession(SESSION_ID, TTL_MINUTES);
        
        // Then - 如果 session 不存在，不应该调用 expire
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }
    
    @Test
    void testRefreshSessionWithZeroTTL() {
        // Given
        Map<String, Object> userProps = new HashMap<>();
        SessionInfo sessionInfo = new SessionInfo(APP_ID, USER_ID, userProps, 0);
        when(valueOperations.get("track:session:" + SESSION_ID)).thenReturn(sessionInfo);
        
        // When
        sessionService.refreshSession(SESSION_ID, 0);
        
        // Then - TTL 为 0 时，应该设置一个很长的过期时间（近似永久）
        verify(redisTemplate).expire(eq("track:session:" + SESSION_ID), 
            any(java.time.Duration.class));
    }
    
    @Test
    void testDeleteSession() {
        // Given
        when(redisTemplate.delete("track:session:" + SESSION_ID)).thenReturn(true);
        
        // When
        sessionService.deleteSession(SESSION_ID);
        
        // Then
        verify(redisTemplate).delete("track:session:" + SESSION_ID);
        verify(valueOperations, never()).get(anyString()); // 确保没有调用 get
    }
    
    @Test
    void testSaveSessionStoresCorrectData() {
        // Given
        Map<String, Object> userProps = new HashMap<>();
        userProps.put("plan", "premium");
        userProps.put("role", "admin");
        
        // When
        sessionService.saveSession(SESSION_ID, APP_ID, USER_ID, userProps, TTL_MINUTES);
        
        // Then - 验证存储的数据正确性
        verify(valueOperations).set(eq("track:session:" + SESSION_ID), argThat(sessionInfo -> {
            SessionInfo info = (SessionInfo) sessionInfo;
            return APP_ID.equals(info.getAppId()) &&
                   USER_ID.equals(info.getUserId()) &&
                   userProps.equals(info.getUserProps()) &&
                   TTL_MINUTES == info.getTtlMinutes();
        }), eq((long) TTL_MINUTES), eq(TimeUnit.MINUTES));
    }
}


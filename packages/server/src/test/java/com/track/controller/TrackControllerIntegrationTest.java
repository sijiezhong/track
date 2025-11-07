package com.track.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.track.dto.SessionInfo;
import com.track.dto.TrackBatchRequest;
import com.track.entity.Project;
import com.track.repository.ProjectRepository;
import com.track.service.RateLimitService;
import com.track.service.SessionService;
import com.track.service.TrackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TrackController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TrackControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private TrackService trackService;
    
    @MockBean
    private SessionService sessionService;
    
    @MockBean
    private RateLimitService rateLimitService;
    
    @MockBean
    private ProjectRepository projectRepository;
    
    private String sessionId;
    private String appId;
    private SessionInfo sessionInfo;
    
    @BeforeEach
    void setUp() {
        sessionId = "test-session-id";
        appId = "test-app";
        
        // Mock SessionInfo
        Map<String, Object> userProps = new HashMap<>();
        userProps.put("plan", "premium");
        sessionInfo = new SessionInfo(appId, "user-123", userProps, 1440);
        
        // Mock Project
        Project project = new Project();
        project.setAppId(appId);
        project.setAppName("Test App");
        project.setIsActive(true);
        when(projectRepository.findByAppId(appId)).thenReturn(Optional.of(project));
        
        // Mock TrackService
        when(trackService.validateAppId(appId)).thenReturn(true);
        
        // Mock RateLimitService
        when(rateLimitService.isAllowed(anyString(), anyString())).thenReturn(true);
        
        // Mock TrackService async method
        doNothing().when(trackService).processBatchEventsAsync(any(TrackBatchRequest.class));
    }
    
    @Test
    void testIngestEvent_AutoRefreshSession() throws Exception {
        // Given
        when(sessionService.getSession(sessionId)).thenReturn(sessionInfo);
        
        TrackBatchRequest request = new TrackBatchRequest();
        request.setE(new java.util.ArrayList<>());
        
        // When & Then
        mockMvc.perform(post("/api/ingest")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .cookie(new jakarta.servlet.http.Cookie("track_session_id", sessionId))
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted());
        
        // 验证 Session 被自动刷新
        verify(sessionService, times(1)).refreshSession(sessionId, 1440);
    }
    
    @Test
    void testIngestEvent_WithoutCookie_Returns401() throws Exception {
        // Given
        TrackBatchRequest request = new TrackBatchRequest();
        request.setE(new java.util.ArrayList<>());
        
        // When & Then
        mockMvc.perform(post("/api/ingest")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
        
        // 验证 Session 没有被刷新（因为没有 Session）
        verify(sessionService, never()).refreshSession(anyString(), anyInt());
    }
    
    @Test
    void testIngestEvent_ExpiredSession_Returns401() throws Exception {
        // Given
        when(sessionService.getSession(sessionId)).thenReturn(null);
        
        TrackBatchRequest request = new TrackBatchRequest();
        request.setE(new java.util.ArrayList<>());
        
        // When & Then
        mockMvc.perform(post("/api/ingest")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .cookie(new jakarta.servlet.http.Cookie("track_session_id", sessionId))
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
        
        // 验证 Session 没有被刷新（因为 Session 不存在）
        verify(sessionService, never()).refreshSession(anyString(), anyInt());
    }
    
    @Test
    void testIngestEvent_InvalidAppId_Returns400() throws Exception {
        // Given
        when(sessionService.getSession(sessionId)).thenReturn(sessionInfo);
        when(trackService.validateAppId(appId)).thenReturn(false);
        
        TrackBatchRequest request = new TrackBatchRequest();
        request.setE(new java.util.ArrayList<>());
        
        // When & Then
        mockMvc.perform(post("/api/ingest")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .cookie(new jakarta.servlet.http.Cookie("track_session_id", sessionId))
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
        
        // 验证 Session 没有被刷新（因为验证失败）
        verify(sessionService, never()).refreshSession(anyString(), anyInt());
    }
    
    @Test
    void testIngestEvent_RateLimitExceeded_Returns429() throws Exception {
        // Given
        when(sessionService.getSession(sessionId)).thenReturn(sessionInfo);
        when(rateLimitService.isAllowed(anyString(), anyString())).thenReturn(false);
        
        TrackBatchRequest request = new TrackBatchRequest();
        request.setE(new java.util.ArrayList<>());
        
        // When & Then
        mockMvc.perform(post("/api/ingest")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .cookie(new jakarta.servlet.http.Cookie("track_session_id", sessionId))
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isTooManyRequests());
        
        // 验证 Session 没有被刷新（因为限流失败）
        verify(sessionService, never()).refreshSession(anyString(), anyInt());
    }
}


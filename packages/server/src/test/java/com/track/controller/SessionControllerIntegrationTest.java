package com.track.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.track.dto.SessionRequest;
import com.track.dto.SessionResponse;
import com.track.dto.SessionInfo;
import com.track.entity.Project;
import com.track.repository.ProjectRepository;
import com.track.service.SessionService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SessionController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SessionControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ProjectRepository projectRepository;
    
    @MockBean
    private SessionService sessionService;
    
    private String appId;
    
    @BeforeEach
    void setUp() {
        appId = "test-app";
        // Mock ProjectRepository
        Project project = new Project();
        project.setAppId(appId);
        project.setAppName("Test App");
        project.setIsActive(true);
        when(projectRepository.findByAppId(appId)).thenReturn(Optional.of(project));
        when(projectRepository.findByAppId("invalid-app")).thenReturn(Optional.empty());
        // 对于自动创建项目的场景，模拟 save 返回新建项目
        when(projectRepository.save(org.mockito.ArgumentMatchers.any(Project.class)))
                .thenAnswer(invocation -> {
                    Project p = invocation.getArgument(0);
                    if (p.getAppName() == null) p.setAppName(p.getAppId());
                    p.setIsActive(true);
                    return p;
                });
        
        // SessionService default stubs
        doNothing().when(sessionService).saveSession(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyMap(), anyInt());
        doNothing().when(sessionService).refreshSession(anyString(), anyInt());
        doNothing().when(sessionService).deleteSession(anyString());
    }
    
    @Test
    void testCreateSession() throws Exception {
        // Given
        SessionRequest request = new SessionRequest();
        request.setAppId(appId);
        request.setUserId("user-123");
        Map<String, Object> userProps = new HashMap<>();
        userProps.put("plan", "premium");
        request.setUserProps(userProps);
        request.setTtlMinutes(1440);
        
        // When & Then
        String response = mockMvc.perform(post("/api/session")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("track_session_id"))
            .andExpect(cookie().httpOnly("track_session_id", true))
            .andExpect(cookie().path("track_session_id", "/"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        SessionResponse sessionResponse = objectMapper.readValue(response, SessionResponse.class);
        assertNotNull(sessionResponse.getSessionId());
        assertFalse(sessionResponse.getSessionId().isEmpty());
    }
    
    @Test
    void testCreateSessionWithUnknownAppId_AutoCreateProject() throws Exception {
        // Given
        SessionRequest request = new SessionRequest();
        request.setAppId("invalid-app");
        request.setAppName("Auto Name");
        request.setUserId("user-123");

        // When & Then：应自动创建项目并成功下发 session
        mockMvc.perform(post("/api/session")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("track_session_id"));
    }

    @Test
    void testCreateSessionWithInactiveProject_Should400() throws Exception {
        // Given：inactive 项目
        String inactiveApp = "inactive-app";
        Project inactiveProject = new Project();
        inactiveProject.setAppId(inactiveApp);
        inactiveProject.setAppName("Inactive");
        inactiveProject.setIsActive(false);
        when(projectRepository.findByAppId(inactiveApp)).thenReturn(Optional.of(inactiveProject));

        SessionRequest request = new SessionRequest();
        request.setAppId(inactiveApp);
        request.setUserId("user-123");
        
        // When & Then
        mockMvc.perform(post("/api/session")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    void testCreateSessionWithZeroTTL() throws Exception {
        // Given
        SessionRequest request = new SessionRequest();
        request.setAppId(appId);
        request.setUserId("user-123");
        request.setTtlMinutes(0); // 不过期
        
        // When & Then
        mockMvc.perform(post("/api/session")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("track_session_id"))
            .andExpect(cookie().maxAge("track_session_id", Integer.MAX_VALUE));
    }
    
    @Test
    void testRefreshSession() throws Exception {
        // Given - 先创建 Session
        SessionRequest createRequest = new SessionRequest();
        createRequest.setAppId(appId);
        createRequest.setUserId("user-123");
        createRequest.setTtlMinutes(1440);
        
        String sessionId = mockMvc.perform(post("/api/session")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookie("track_session_id")
            .getValue();
        
        // Stub: 刚创建的 session 应该能被读取
        when(sessionService.getSession(sessionId)).thenReturn(new SessionInfo(appId, "user-123", new HashMap<>(), 1440));
        
        // When & Then - 刷新 Session
        mockMvc.perform(post("/api/session/refresh")
                .cookie(new jakarta.servlet.http.Cookie("track_session_id", sessionId)))
            .andExpect(status().isOk())
            .andExpect(cookie().exists("track_session_id"));
    }
    
    @Test
    void testRefreshSessionWithoutCookie() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/session/refresh"))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testRefreshSessionWithInvalidSessionId() throws Exception {
        // Given
        when(sessionService.getSession("invalid-session-id")).thenReturn(null);
        
        // When & Then
        mockMvc.perform(post("/api/session/refresh")
                .cookie(new jakarta.servlet.http.Cookie("track_session_id", "invalid-session-id")))
            .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testDestroySession() throws Exception {
        // Given - 先创建 Session
        SessionRequest createRequest = new SessionRequest();
        createRequest.setAppId(appId);
        createRequest.setUserId("user-123");
        
        String sessionId = mockMvc.perform(post("/api/session")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getCookie("track_session_id")
            .getValue();
        
        // When & Then - 销毁 Session
        mockMvc.perform(post("/api/session/destroy")
                .cookie(new jakarta.servlet.http.Cookie("track_session_id", sessionId)))
            .andExpect(status().isOk())
            .andExpect(cookie().maxAge("track_session_id", 0));
    }
    
    @Test
    void testSessionDataCorrectness() throws Exception {
        // Given
        SessionRequest request = new SessionRequest();
        request.setAppId(appId);
        request.setUserId("user-123");
        Map<String, Object> userProps = new HashMap<>();
        userProps.put("plan", "premium");
        userProps.put("role", "admin");
        request.setUserProps(userProps);
        request.setTtlMinutes(1440);
        
        // When
        String response = mockMvc.perform(post("/api/session")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        
        SessionResponse sessionResponse = objectMapper.readValue(response, SessionResponse.class);
        String sessionId = sessionResponse.getSessionId();
        
        // Then - 验证 Session 数据正确性（通过刷新接口验证）
        when(sessionService.getSession(sessionId)).thenReturn(new SessionInfo(appId, "user-123", userProps, 1440));
        mockMvc.perform(post("/api/session/refresh")
                .cookie(new jakarta.servlet.http.Cookie("track_session_id", sessionId)))
            .andExpect(status().isOk());
    }
}

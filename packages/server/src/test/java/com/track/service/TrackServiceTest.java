package com.track.service;

import com.track.dto.SessionInfo;
import com.track.entity.Project;
import com.track.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TrackService 单元测试
 * 验证事件处理逻辑的正确性
 */
@ExtendWith(MockitoExtension.class)
class TrackServiceTest {
    
    @Mock
    private ProjectRepository projectRepository;
    
    @Mock
    private com.track.repository.EventRepository eventRepository;
    
    @InjectMocks
    private TrackService trackService;
    
    private static final String APP_ID = "test-app";
    private static final String USER_ID = "user-123";
    
    @BeforeEach
    void setUp() {
        // 不在 setUp 中设置默认 mock，避免不必要的 stubbing
    }
    
    @Test
    void testValidateAppIdExistsAndActive() {
        // Given
        Project activeProject = new Project();
        activeProject.setAppId(APP_ID);
        activeProject.setIsActive(true);
        when(projectRepository.findByAppId(APP_ID)).thenReturn(Optional.of(activeProject));
        
        // When
        boolean result = trackService.validateAppId(APP_ID);
        
        // Then
        assertTrue(result);
        verify(projectRepository).findByAppId(APP_ID);
    }
    
    @Test
    void testValidateAppIdNotExists() {
        // Given
        when(projectRepository.findByAppId("invalid-app")).thenReturn(Optional.empty());
        
        // When
        boolean result = trackService.validateAppId("invalid-app");
        
        // Then
        assertFalse(result);
        verify(projectRepository).findByAppId("invalid-app");
    }
    
    @Test
    void testValidateAppIdInactive() {
        // Given
        Project inactiveProject = new Project();
        inactiveProject.setAppId("inactive-app");
        inactiveProject.setIsActive(false);
        when(projectRepository.findByAppId("inactive-app")).thenReturn(Optional.of(inactiveProject));
        
        // When
        boolean result = trackService.validateAppId("inactive-app");
        
        // Then
        assertFalse(result);
        verify(projectRepository).findByAppId("inactive-app");
    }
}


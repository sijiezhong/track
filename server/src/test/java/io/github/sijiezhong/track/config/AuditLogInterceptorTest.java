package io.github.sijiezhong.track.config;

import io.github.sijiezhong.track.domain.AuditLog;
import io.github.sijiezhong.track.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogInterceptor.
 * 
 * Coverage includes:
 * - GET requests bypassing logging
 * - Excluded paths (collect endpoints)
 * - Normal POST/PUT/DELETE requests creating audit logs
 * - Tenant ID and username extraction
 * - Invalid tenant header handling
 * 
 * Note: Integration tests are in AuditLogInterceptorIntegrationTest.
 */
public class AuditLogInterceptorTest {

    private AuditLogRepository repository;
    private AuditLogInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(AuditLogRepository.class);
        interceptor = new AuditLogInterceptor(repository);
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        // Clean security context between tests
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should bypass logging for GET requests")
    void should_BypassLogging_ForGetRequests() {
        when(request.getMethod()).thenReturn("GET");
        boolean pre = interceptor.preHandle(request, response, this);
        interceptor.afterCompletion(request, response, this, null);
        assertThat(pre).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should bypass logging for excluded collect path")
    void should_BypassLogging_ForExcludedCollectPath() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/events/collect/batch");
        boolean pre = interceptor.preHandle(request, response, this);
        interceptor.afterCompletion(request, response, this, null);
        assertThat(pre).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should save audit log for POST with tenant and user")
    void should_SaveAuditLog_ForPostWithTenantAndUser() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/admin/apps");
        when(request.getHeader("X-Tenant-Id")).thenReturn("42");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("alice", "n/a"));
        SecurityContextHolder.setContext(context);

        interceptor.preHandle(request, response, this);
        interceptor.afterCompletion(request, response, this, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository, times(1)).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(42);
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getMethod()).isEqualTo("POST");
        assertThat(saved.getPath()).isEqualTo("/api/v1/admin/apps");
        assertThat(saved.getPayload()).isNotNull();
        assertThat(saved.getCreateTime()).isNotNull();
    }

    @Test
    @DisplayName("Should handle invalid tenant header and save with null tenant")
    void should_HandleInvalidTenantHeader_AndSaveWithNullTenant() {
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/v1/admin/apps/1");
        when(request.getHeader("X-Tenant-Id")).thenReturn("not-a-number");

        SecurityContextHolder.clearContext(); // anonymous user

        interceptor.preHandle(request, response, this);
        interceptor.afterCompletion(request, response, this, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository, times(1)).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getTenantId()).isNull();
        assertThat(saved.getUsername()).isEqualTo("anonymous");
        assertThat(saved.getMethod()).isEqualTo("DELETE");
        assertThat(saved.getPath()).isEqualTo("/api/v1/admin/apps/1");
    }

    @Test
    @DisplayName("Should save audit log for PUT requests")
    void should_SaveAuditLog_ForPutRequests() {
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/api/v1/admin/apps/1");
        when(request.getHeader("X-Tenant-Id")).thenReturn("10");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("bob", "n/a"));
        SecurityContextHolder.setContext(context);

        interceptor.preHandle(request, response, this);
        interceptor.afterCompletion(request, response, this, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository, times(1)).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(10);
        assertThat(saved.getMethod()).isEqualTo("PUT");
    }

    @Test
    @DisplayName("Should bypass logging for collect path")
    void should_BypassLogging_ForCollectPath() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/events/collect");
        interceptor.preHandle(request, response, this);
        interceptor.afterCompletion(request, response, this, null);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Should parse valid tenant header number")
    void should_ParseValidTenantHeader_Number() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/admin/apps");
        when(request.getHeader("X-Tenant-Id")).thenReturn("99");

        SecurityContextHolder.clearContext();

        interceptor.preHandle(request, response, this);
        interceptor.afterCompletion(request, response, this, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository, times(1)).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(99);
    }
}

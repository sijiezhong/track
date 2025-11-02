package io.github.sijiezhong.track.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TenantGuardInterceptor.
 * 
 * Coverage includes:
 * - Path matching logic
 * - Header validation
 * - Protected vs unprotected paths
 * 
 * Note: Integration tests are in TenantGuardIntegrationTest.
 */
public class TenantGuardInterceptorTest {

    private TenantGuardInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new TenantGuardInterceptor();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
    }

    @Test
    @DisplayName("Should allow non-protected paths")
    void should_AllowNonProtectedPaths() {
        when(request.getRequestURI()).thenReturn("/api/v1/events/collect");
        boolean result = interceptor.preHandle(request, response, null);
        assertThat(result).isTrue();
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Should require tenant header for admin path")
    void should_RequireTenantHeader_ForAdminPath() {
        when(request.getRequestURI()).thenReturn("/api/v1/admin/apps");
        when(request.getHeader("X-Tenant-Id")).thenReturn(null);
        boolean result = interceptor.preHandle(request, response, null);
        assertThat(result).isFalse();
        verify(response, times(1)).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("Should require tenant header for analytics path")
    void should_RequireTenantHeader_ForAnalyticsPath() {
        when(request.getRequestURI()).thenReturn("/api/analytics/trend");
        when(request.getHeader("X-Tenant-Id")).thenReturn(" ");
        boolean result = interceptor.preHandle(request, response, null);
        assertThat(result).isFalse();
        verify(response, times(1)).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    @DisplayName("Should require tenant header for export path")
    void should_RequireTenantHeader_ForExportPath() {
        when(request.getRequestURI()).thenReturn("/api/v1/events/export/csv");
        when(request.getHeader("X-Tenant-Id")).thenReturn("1");
        boolean result = interceptor.preHandle(request, response, null);
        assertThat(result).isTrue();
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Should require tenant header for webhook path")
    void should_RequireTenantHeader_ForWebhookPath() {
        when(request.getRequestURI()).thenReturn("/api/webhook/subscriptions");
        when(request.getHeader("X-Tenant-Id")).thenReturn("1");
        boolean result = interceptor.preHandle(request, response, null);
        assertThat(result).isTrue();
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    @DisplayName("Should allow with valid tenant header")
    void should_Allow_WithValidTenantHeader() {
        when(request.getRequestURI()).thenReturn("/api/v1/admin/apps");
        when(request.getHeader("X-Tenant-Id")).thenReturn("42");
        boolean result = interceptor.preHandle(request, response, null);
        assertThat(result).isTrue();
        verify(response, never()).setStatus(anyInt());
    }
}

package io.github.sijiezhong.track.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WebConfigTest {

    @Test
    void addInterceptorsShouldRegisterAllWhenPresent() {
        AuditLogInterceptor auditLogInterceptor = mock(AuditLogInterceptor.class);
        TenantGuardInterceptor tenantGuardInterceptor = mock(TenantGuardInterceptor.class);
        LoggingInterceptor loggingInterceptor = mock(LoggingInterceptor.class);
        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);
        
        when(registry.addInterceptor(any(HandlerInterceptor.class))).thenReturn(registration);
        when(registration.addPathPatterns(any(String[].class))).thenReturn(registration);
        
        WebConfig config = new WebConfig(auditLogInterceptor, tenantGuardInterceptor, loggingInterceptor);
        config.addInterceptors(registry);
        
        // 日志拦截器在最前面
        verify(registry, times(1)).addInterceptor(eq(loggingInterceptor));
        verify(registry, times(1)).addInterceptor(eq(tenantGuardInterceptor));
        verify(registry, times(1)).addInterceptor(eq(auditLogInterceptor));
        verify(registration, times(3)).addPathPatterns(eq("/api/**"));
    }

    @Test
    void addInterceptorsShouldSkipTenantGuardWhenNull() {
        AuditLogInterceptor auditLogInterceptor = mock(AuditLogInterceptor.class);
        LoggingInterceptor loggingInterceptor = mock(LoggingInterceptor.class);
        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);
        
        when(registry.addInterceptor(any(HandlerInterceptor.class))).thenReturn(registration);
        when(registration.addPathPatterns(any(String[].class))).thenReturn(registration);
        
        WebConfig config = new WebConfig(auditLogInterceptor, null, loggingInterceptor);
        config.addInterceptors(registry);
        
        verify(registry, times(1)).addInterceptor(eq(loggingInterceptor));
        verify(registry, never()).addInterceptor(any(TenantGuardInterceptor.class));
        verify(registry, times(1)).addInterceptor(eq(auditLogInterceptor));
        verify(registration, times(2)).addPathPatterns(eq("/api/**"));
    }

    @Test
    void addInterceptorsShouldSkipAuditLogWhenNull() {
        TenantGuardInterceptor tenantGuardInterceptor = mock(TenantGuardInterceptor.class);
        LoggingInterceptor loggingInterceptor = mock(LoggingInterceptor.class);
        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);
        
        when(registry.addInterceptor(any(HandlerInterceptor.class))).thenReturn(registration);
        when(registration.addPathPatterns(any(String[].class))).thenReturn(registration);
        
        WebConfig config = new WebConfig(null, tenantGuardInterceptor, loggingInterceptor);
        config.addInterceptors(registry);
        
        verify(registry, times(1)).addInterceptor(eq(loggingInterceptor));
        verify(registry, times(1)).addInterceptor(eq(tenantGuardInterceptor));
        verify(registry, never()).addInterceptor(any(AuditLogInterceptor.class));
        verify(registration, times(2)).addPathPatterns(eq("/api/**"));
    }

    @Test
    void addInterceptorsShouldRegisterOnlyLoggingWhenOthersNull() {
        LoggingInterceptor loggingInterceptor = mock(LoggingInterceptor.class);
        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);
        
        when(registry.addInterceptor(any(HandlerInterceptor.class))).thenReturn(registration);
        when(registration.addPathPatterns(any(String[].class))).thenReturn(registration);
        
        WebConfig config = new WebConfig(null, null, loggingInterceptor);
        config.addInterceptors(registry);
        
        verify(registry, times(1)).addInterceptor(eq(loggingInterceptor));
        verify(registry, never()).addInterceptor(any(TenantGuardInterceptor.class));
        verify(registry, never()).addInterceptor(any(AuditLogInterceptor.class));
        verify(registration, times(1)).addPathPatterns(eq("/api/**"));
    }

    @Test
    void constructorShouldAcceptNullInterceptors() {
        // Test that constructor doesn't throw when interceptors are null
        WebConfig config1 = new WebConfig(null, null, null);
        assertThat(config1).isNotNull();
        
        LoggingInterceptor loggingInterceptor = mock(LoggingInterceptor.class);
        WebConfig config2 = new WebConfig(mock(AuditLogInterceptor.class), null, loggingInterceptor);
        assertThat(config2).isNotNull();
        
        WebConfig config3 = new WebConfig(null, mock(TenantGuardInterceptor.class), loggingInterceptor);
        assertThat(config3).isNotNull();
    }
    
    @Test
    void addInterceptorsShouldSkipAllWhenAllNull() {
        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        
        WebConfig config = new WebConfig(null, null, null);
        config.addInterceptors(registry);
        
        verify(registry, never()).addInterceptor(any(HandlerInterceptor.class));
    }
}

package io.github.sijiezhong.track.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@ConditionalOnProperty(name = "tenant.guard.enabled", havingValue = "true", matchIfMissing = false)
public class TenantGuardInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String path = request.getRequestURI();
        // 仅保护敏感接口：
        // 1. 旧路径（向后兼容）：/api/admin, /api/analytics（包括所有子路径），/api/events/export, /api/webhook
        // 2. 新路径的管理员和导出接口：/api/v1/admin, /api/v1/webhooks, /api/v1/events/export
        // 注意：新路径的分析接口（/api/v1/events/trend等）不在拦截器保护范围内，让Spring Security先处理认证
        boolean isProtectedPath = path.startsWith("/api/v1/admin") || path.startsWith("/api/v1/webhooks") 
            || path.startsWith("/api/v1/events/export")
            // 向后兼容旧路径（注意：/api/analytics及其所有子路径都需要保护）
            || path.startsWith("/api/admin") || path.startsWith("/api/analytics") 
            || path.startsWith("/api/events/export") || path.startsWith("/api/webhook");
        
        if (!isProtectedPath) {
            return true;
        }
        
        String tenantHeader = request.getHeader("X-Tenant-Id");
        if (tenantHeader == null || tenantHeader.isBlank()) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        // 体内校验交由各 Controller 自身完成，避免读取请求体引发流冲突
        return true;
    }
}



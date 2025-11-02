package io.github.sijiezhong.track.config;

import io.github.sijiezhong.track.domain.AuditLog;
import io.github.sijiezhong.track.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.BufferedReader;
import java.time.LocalDateTime;

@Component
@ConditionalOnBean(io.github.sijiezhong.track.repository.AuditLogRepository.class)
@ConditionalOnProperty(prefix = "audit", name = "enabled", havingValue = "true", matchIfMissing = false)
public class AuditLogInterceptor implements HandlerInterceptor {
    private final AuditLogRepository repository;

    public AuditLogInterceptor(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"DELETE".equals(method)) {
            return true;
        }
        // 推迟到 afterCompletion 记录，避免与控制器读写冲突
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod();
        if (!"POST".equals(method) && !"PUT".equals(method) && !"DELETE".equals(method)) {
            return;
        }
        try {
            String path = request.getRequestURI();
            // 排除事件收集路径（新旧路径都排除）
            if (path.startsWith("/api/events/collect") || path.startsWith("/api/v1/events/collect")) return;
            Integer tenantId = null;
            try {
                String header = request.getHeader("X-Tenant-Id");
                if (header != null) tenantId = Integer.parseInt(header);
            } catch (Exception ignored) {}
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String user = auth == null ? "anonymous" : String.valueOf(auth.getPrincipal());
            AuditLog log = new AuditLog();
            log.setTenantId(tenantId);
            log.setUsername(user);
            log.setMethod(method);
            log.setPath(path);
            log.setPayload("");
            log.setCreateTime(LocalDateTime.now());
            repository.save(log);
        } catch (Exception ignored) {}
    }
}



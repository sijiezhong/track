package io.github.sijiezhong.track.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 
 * <p>配置拦截器、跨域等Web相关设置。
 * 
 * @author sijie
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuditLogInterceptor auditLogInterceptor;
    private final TenantGuardInterceptor tenantGuardInterceptor;
    private final LoggingInterceptor loggingInterceptor;

    public WebConfig(
            @org.springframework.beans.factory.annotation.Autowired(required = false) AuditLogInterceptor auditLogInterceptor,
            @org.springframework.beans.factory.annotation.Autowired(required = false) TenantGuardInterceptor tenantGuardInterceptor,
            @org.springframework.beans.factory.annotation.Autowired(required = false) LoggingInterceptor loggingInterceptor) {
        this.auditLogInterceptor = auditLogInterceptor;
        this.tenantGuardInterceptor = tenantGuardInterceptor;
        this.loggingInterceptor = loggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 日志拦截器放在最前面，确保所有请求都被记录
        if (loggingInterceptor != null) {
            registry.addInterceptor(loggingInterceptor).addPathPatterns("/api/**").order(1);
        }
        
        // TenantGuardInterceptor应该在安全框架之前执行，但要在日志拦截器之后
        // 使用order确保执行顺序：日志(1) -> TenantGuard(2) -> Audit(3)
        if (tenantGuardInterceptor != null) {
            registry.addInterceptor(tenantGuardInterceptor).addPathPatterns("/api/**").order(2);
        }
        if (auditLogInterceptor != null) {
            registry.addInterceptor(auditLogInterceptor).addPathPatterns("/api/**").order(3);
        }
    }
}

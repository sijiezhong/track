package io.github.sijiezhong.track.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

/**
 * 请求日志拦截器
 * 
 * <p>记录所有HTTP请求和响应的详细信息，包括：
 * - 请求URL、方法、参数
 * - 响应状态码、耗时
 * - 异常信息（如果有）
 * 
 * <p>使用MDC（Mapped Diagnostic Context）记录请求ID，便于日志追踪。
 * 
 * @author sijie
 */
public class LoggingInterceptor implements HandlerInterceptor {
    
    /**
     * 创建LoggingInterceptor Bean
     * 
     * @return LoggingInterceptor实例
     */
    @Configuration
    public static class LoggingInterceptorConfig {
        @Bean
        public LoggingInterceptor loggingInterceptor() {
            return new LoggingInterceptor();
        }
    }
    
    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String START_TIME_KEY = "startTime";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 生成请求ID
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(REQUEST_ID_KEY, requestId);
        
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_KEY, startTime);
        
        // 记录请求信息
        StringBuilder requestInfo = new StringBuilder();
        requestInfo.append("请求开始 - ")
            .append("请求ID: ").append(requestId).append(", ")
            .append("方法: ").append(request.getMethod()).append(", ")
            .append("URL: ").append(request.getRequestURI());
        
        String queryString = request.getQueryString();
        if (queryString != null) {
            requestInfo.append("?").append(queryString);
        }
        
        log.info(requestInfo.toString());
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                          Object handler, @Nullable ModelAndView modelAndView) {
        // 此方法在Controller处理完成后、视图渲染前调用
        // 主要用于记录响应信息
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, @Nullable Exception ex) {
        // 计算请求耗时
        Long startTime = (Long) request.getAttribute(START_TIME_KEY);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
        
        // 记录响应信息
        StringBuilder responseInfo = new StringBuilder();
        responseInfo.append("请求完成 - ")
            .append("请求ID: ").append(MDC.get(REQUEST_ID_KEY)).append(", ")
            .append("状态码: ").append(response.getStatus()).append(", ")
            .append("耗时: ").append(duration).append("ms");
        
        if (ex != null) {
            responseInfo.append(", 异常: ").append(ex.getClass().getSimpleName())
                .append(" - ").append(ex.getMessage());
            log.error(responseInfo.toString(), ex);
        } else {
            log.info(responseInfo.toString());
        }
        
        // 清理MDC
        MDC.remove(REQUEST_ID_KEY);
    }
}


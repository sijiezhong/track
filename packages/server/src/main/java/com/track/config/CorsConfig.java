package com.track.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 跨域配置
 * 支持客户端 SDK 的跨域请求，必须配置 allowCredentials: true 以支持 Cookie
 */
@Configuration
public class CorsConfig {
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Session 接口（注册、刷新、销毁）
                registry.addMapping("/api/session/**")
                    .allowedOriginPatterns("*") // 开发环境允许所有来源，生产环境应配置具体域名
                    .allowedMethods("POST", "OPTIONS")
                    .allowedHeaders("Content-Type")
                    .allowCredentials(true) // 重要：必须支持 Cookie
                    .maxAge(3600);
                
                // 数据上报接口
                registry.addMapping("/api/ingest")
                    .allowedOriginPatterns("*") // 开发环境允许所有来源，生产环境应配置具体域名
                    .allowedMethods("POST", "OPTIONS")
                    .allowedHeaders("Content-Type")
                    .allowCredentials(true) // 重要：必须支持 Cookie（用于传递 sessionId）
                    .maxAge(3600);
            }
        };
    }
}


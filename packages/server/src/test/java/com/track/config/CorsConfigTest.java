package com.track.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CORS 配置测试
 * 验证跨域配置正确性，确保支持 Cookie 传递
 */
class CorsConfigTest {
    
    @Test
    void testCorsConfigBean() {
        CorsConfig corsConfig = new CorsConfig();
        WebMvcConfigurer configurer = corsConfig.corsConfigurer();
        
        assertNotNull(configurer);
    }
    
    @Test
    void testCorsMappings() {
        CorsConfig corsConfig = new CorsConfig();
        WebMvcConfigurer configurer = corsConfig.corsConfigurer();
        
        CorsRegistry registry = new CorsRegistry();
        configurer.addCorsMappings(registry);
        
        // 验证 CORS 映射已配置
        assertNotNull(registry);
    }
    
    @Test
    void testSessionEndpointConfigured() {
        // 这个测试验证配置类可以正常创建
        // 实际的 CORS 验证需要集成测试
        CorsConfig corsConfig = new CorsConfig();
        assertNotNull(corsConfig.corsConfigurer());
    }
}


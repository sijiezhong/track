package com.track.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Swagger 配置测试
 * 验证 OpenAPI 配置正确性
 */
class SwaggerConfigTest {
    
    @Test
    void testOpenAPIBean() {
        SwaggerConfig swaggerConfig = new SwaggerConfig();
        OpenAPI openAPI = swaggerConfig.customOpenAPI();
        
        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("Track API", openAPI.getInfo().getTitle());
        assertEquals("1.0", openAPI.getInfo().getVersion());
        assertTrue(openAPI.getInfo().getDescription().contains("埋点"));
    }
    
    @Test
    void testSecurityScheme() {
        SwaggerConfig swaggerConfig = new SwaggerConfig();
        OpenAPI openAPI = swaggerConfig.customOpenAPI();
        
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getComponents().getSecuritySchemes());
        assertTrue(openAPI.getComponents().getSecuritySchemes().containsKey("bearerAuth"));
    }
}


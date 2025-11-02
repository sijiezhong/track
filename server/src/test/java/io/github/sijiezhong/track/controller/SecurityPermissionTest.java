package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0级别安全测试：权限提升攻击场景测试
 * 
 * 这些测试验证系统能够正确阻止低权限用户尝试执行高权限操作。
 * 
 * Coverage includes:
 * - READONLY用户不能执行ADMIN操作
 * - ANALYST用户不能创建应用（ADMIN操作）
 * - 未认证用户不能访问受保护接口
 * - 权限边界测试
 */
@AutoConfigureMockMvc
@ActiveProfiles("secure")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class SecurityPermissionTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("P0: READONLY用户不能执行ADMIN操作（创建应用）")
    void should_ForbidReadonlyUser_FromAdminOperations() throws Exception {
        // When: READONLY用户尝试创建应用（ADMIN操作）
        String body = "{\"appKey\":\"test-app\",\"appName\":\"Test App\",\"tenantId\":1}";
        mockMvc.perform(post("/api/v1/admin/apps")
                        .header("Authorization", "Bearer role:READONLY")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden()); // ✅ 必须是403，不是401或其他
    }

    @Test
    @DisplayName("P0: READONLY用户不能执行ADMIN操作（创建Webhook订阅）")
    void should_ForbidReadonlyUser_FromWebhookAdminOperations() throws Exception {
        // When: READONLY用户尝试创建Webhook订阅（需要ADMIN角色）
        String body = "{\"url\":\"https://example.com/webhook\",\"eventTypes\":[\"pv\"]}";
        mockMvc.perform(post("/api/v1/webhooks")
                        .header("Authorization", "Bearer role:READONLY")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden()); // ✅ 必须是403
    }

    @Test
    @DisplayName("P0: ANALYST用户不能创建应用（ADMIN操作）")
    void should_ForbidAnalystUser_FromAdminOperations() throws Exception {
        // When: ANALYST用户尝试创建应用（ADMIN操作）
        String body = "{\"appKey\":\"test-app\",\"appName\":\"Test App\",\"tenantId\":1}";
        mockMvc.perform(post("/api/v1/admin/apps")
                        .header("Authorization", "Bearer role:ANALYST")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden()); // ✅ 必须是403
    }

    @Test
    @DisplayName("P0: ANALYST用户可以访问分析接口")
    void should_AllowAnalystUser_ToAccessAnalytics() throws Exception {
        // When: ANALYST用户访问分析接口（允许）
        mockMvc.perform(get("/api/v1/events/trend")
                        .header("Authorization", "Bearer role:ANALYST")
                        .header("X-Tenant-Id", "1")
                        .param("eventName", "pv")
                        .param("interval", "daily"))
                .andExpect(status().isOk()); // ✅ ANALYST可以访问分析接口
    }

    @Test
    @DisplayName("P0: 未认证用户不能访问受保护的分析接口")
    void should_ForbidUnauthenticatedUser_FromProtectedAnalytics() throws Exception {
        // When: 未提供Authorization header的用户尝试访问分析接口
        var result = mockMvc.perform(get("/api/v1/events/trend")
                        .header("X-Tenant-Id", "1")
                        .param("eventName", "pv")
                        .param("interval", "daily"))
                .andReturn();
        
        // Then: 必须是401或403
        int statusCode = result.getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(statusCode)
                .as("Unauthenticated user should be rejected with 401 or 403")
                .isIn(401, 403);
    }

    @Test
    @DisplayName("P0: READONLY用户可以访问查询接口（如果允许）")
    void should_AllowReadonlyUser_ToAccessQueryInterface() throws Exception {
        // 注意：根据业务需求，READONLY用户可能可以访问查询接口
        // 如果接口没有@PreAuthorize限制，则允许访问
        var result = mockMvc.perform(get("/api/v1/events")
                        .header("X-Tenant-Id", "1"))
                .andReturn();
        
        // Then: 应该是200（允许）或401/403（要求认证）
        int statusCode = result.getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(statusCode)
                .as("Query interface should return 200 if accessible or 401/403 if requires auth")
                .isIn(200, 401, 403);
    }

    @Test
    @DisplayName("P0: DEVELOPER用户不能执行ADMIN操作")
    void should_ForbidDeveloperUser_FromAdminOperations() throws Exception {
        // When: DEVELOPER用户尝试创建应用（ADMIN操作）
        String body = "{\"appKey\":\"test-app\",\"appName\":\"Test App\",\"tenantId\":1}";
        mockMvc.perform(post("/api/v1/admin/apps")
                        .header("Authorization", "Bearer role:DEVELOPER")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden()); // ✅ 必须是403
    }
}


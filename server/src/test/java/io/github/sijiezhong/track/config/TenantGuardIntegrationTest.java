package io.github.sijiezhong.track.config;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for TenantGuardInterceptor.
 * 
 * These tests verify the interceptor behavior in the full Spring context
 * with real HTTP requests.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.mvc.throw-exception-if-no-handler-found=true",
    "spring.web.resources.add-mappings=false",
    "tenant.guard.enabled=true"
})
public class TenantGuardIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("Path Coverage Tests")
    class PathCoverageTests {

        @Test
        @DisplayName("Should pass non-guarded collect endpoint without tenant header")
        void should_PassNonGuardedCollect_WithoutTenantHeader() throws Exception {
            String body = "{\n" +
                    "  \"eventName\": \"page_view\",\n" +
                    "  \"sessionId\": \"sess-guard-1\",\n" +
                    "  \"tenantId\": 1\n" +
                    "}";
            mockMvc.perform(post("/api/v1/events/collect")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should forbid guarded export without tenant header")
        @WithMockUser(roles = {"ADMIN"})
        void should_ForbidGuardedExport_WithoutTenantHeader() throws Exception {
            mockMvc.perform(get("/api/v1/events/export.csv"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Analytics Paths Tests")
    class AnalyticsPathsTests {

        @Test
        @DisplayName("Should return bad request for analytics without tenant header")
        void should_ReturnBadRequest_ForAnalyticsWithoutTenantHeader() throws Exception {
            mockMvc.perform(get("/api/v1/events/segmentation")
                            .param("eventName", "pv")
                            .param("by", "browser"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should pass analytics with tenant header")
        void should_PassAnalytics_WithTenantHeader() throws Exception {
            mockMvc.perform(get("/api/v1/events/segmentation")
                            .header("X-Tenant-Id", 1)
                            .param("eventName", "pv")
                            .param("by", "browser"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should forbid synthetic analytics path without tenant header")
        void should_ForbidSyntheticAnalyticsPath_WithoutTenantHeader() throws Exception {
            // Even though no controller handles it, the guard should block it
            mockMvc.perform(get("/api/analytics/foo"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reach 404 for synthetic analytics path with tenant header")
        void should_Reach404_ForSyntheticAnalyticsPathWithTenantHeader() throws Exception {
            // With tenant header present, guard passes and we get 404 from no handler
            mockMvc.perform(get("/api/analytics/foo").header("X-Tenant-Id", 1))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Webhook Paths Tests")
    class WebhookPathsTests {

        @Test
        @DisplayName("Should forbid webhook create without tenant header")
        void should_ForbidWebhookCreate_WithoutTenantHeader() throws Exception {
            String json = "{\n" +
                    "  \"url\": \"https://example.com/hook\",\n" +
                    "  \"secret\": \"s\"\n" +
                    "}";
            mockMvc.perform(post("/api/v1/webhooks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should create webhook with tenant header")
        void should_CreateWebhook_WithTenantHeader() throws Exception {
            String json = "{\n" +
                    "  \"url\": \"https://example.com/hook\",\n" +
                    "  \"secret\": \"s\"\n" +
                    "}";
            mockMvc.perform(post("/api/v1/webhooks")
                            .header("X-Tenant-Id", 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated());
        }
    }
}


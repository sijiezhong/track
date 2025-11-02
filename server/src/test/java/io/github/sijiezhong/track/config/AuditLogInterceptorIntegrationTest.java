package io.github.sijiezhong.track.config;

import io.github.sijiezhong.track.repository.AuditLogRepository;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for AuditLogInterceptor.
 * 
 * These tests verify the interceptor behavior in the full Spring context
 * with real HTTP requests and database operations.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration",
    "tenant.guard.enabled=false"
})
public class AuditLogInterceptorIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Nested
    @DisplayName("GET Request Exclusion Tests")
    class GetExcludedTests {

        @Test
        @DisplayName("Should not audit GET requests")
        void should_NotAuditGetRequests() throws Exception {
            long before = auditLogRepository.count();
            // A GET to analytics without params will 400, but must not create audit logs (GET is excluded)
            mockMvc.perform(get("/api/v1/events/segmentation")
                            .param("eventName", "pv")
                            .param("by", "browser"))
                    .andExpect(status().isBadRequest());
            long after = auditLogRepository.count();
            Assertions.assertEquals(before, after);
        }
    }

    @Nested
    @DisplayName("Write Actions Tests")
    class WriteActionsTests {

        @Test
        @DisplayName("Should create audit log for admin POST")
        void should_CreateAuditLog_ForAdminPost() throws Exception {
            int before = (int) auditLogRepository.count();
            String body = "{\n" +
                    "  \"appKey\": \"app-put\",\n" +
                    "  \"appName\": \"app-put\",\n" +
                    "  \"tenantId\": 1\n" +
                    "}";
            mockMvc.perform(post("/api/v1/admin/apps")
                            .header("X-Tenant-Id", 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().is2xxSuccessful());

            int after = (int) auditLogRepository.count();
            Assertions.assertTrue(after > before);
        }
    }

    @Nested
    @DisplayName("Path Exclusion Tests")
    class ExclusionTests {

        @Test
        @DisplayName("Should not create audit log for collect endpoint")
        void should_NotCreateAuditLog_ForCollectEndpoint() throws Exception {
            String body = "{\n" +
                    "  \"eventName\": \"page_view\",\n" +
                    "  \"sessionId\": \"sess-audit-ex-1\",\n" +
                    "  \"tenantId\": 1\n" +
                    "}";
            mockMvc.perform(post("/api/v1/events/collect")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());

            Assertions.assertTrue(auditLogRepository.findAll().isEmpty());
        }
    }
}


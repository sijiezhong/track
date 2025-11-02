package io.github.sijiezhong.track.config;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for configuration beans and their actual behavior.
 * 
 * Instead of just checking bean existence, these tests verify that:
 * - Tenant guard actually intercepts requests and validates tenantId
 * - Audit log interceptor actually records audit logs (when enabled)
 */
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class ConfigBeansConditionTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should reject request when tenantId in header mismatches tenantId in body")
    void tenantGuardShouldRejectMismatchedTenant() throws Exception {
        /**
         * Tests that tenant guard interceptor actually works by verifying:
         * - Requests with mismatched tenantId are rejected (403)
         * - Requests with matching tenantId are allowed (201)
         */

        // Given: Request with header tenantId=9, but body tenantId=8
        String body = "{\"eventName\":\"pv\",\"sessionId\":\"s1\",\"tenantId\":8}";

        // When: Send request
        // Then: Should return 403 Forbidden
        mockMvc.perform(post("/api/v1/events/collect")
                .header("X-Tenant-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow request when tenantId in header matches tenantId in body")
    void tenantGuardShouldAllowMatchingTenant() throws Exception {
        // Given: Request with header tenantId=9 and body tenantId=9 (matching)
        String body = "{\"eventName\":\"pv\",\"sessionId\":\"s1\",\"tenantId\":9}";

        // When: Send request
        // Then: Should succeed (201 Created)
        mockMvc.perform(post("/api/v1/events/collect")
                .header("X-Tenant-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should allow request when tenantId is only in header")
    void tenantGuardShouldAllowTenantIdInHeaderOnly() throws Exception {
        // Given: Request with tenantId only in header, not in body
        String body = "{\"eventName\":\"pv\",\"sessionId\":\"s1\"}";

        // When: Send request
        // Then: Should succeed (header tenantId should be applied)
        mockMvc.perform(post("/api/v1/events/collect")
                .header("X-Tenant-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }
}

/**
 * Test class for tenant guard disabled scenario.
 * When tenant.guard.enabled=false, tenant guard should not be active.
 */
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestPropertySource(properties = {
        "tenant.guard.enabled=false"
})
class ConfigBeansConditionTenantDisabledTest extends PostgresTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should still reject mismatched tenant when tenant guard is disabled but controller validates")
    void tenantGuardDisabledButControllerStillValidates() throws Exception {
        /**
         * Note: Even when TenantGuardInterceptor is disabled, EventController itself
         * performs tenant validation (lines 79-81 in EventController.java).
         * So mismatched tenantIds are still rejected at the controller level.
         */
        // Given: Request with mismatched tenantId (header=9, body=8)
        String body = "{\"eventName\":\"pv\",\"sessionId\":\"s1\",\"tenantId\":8}";

        // When: Send request with tenant guard disabled
        // Then: Should still be rejected by controller-level validation
        mockMvc.perform(post("/api/v1/events/collect")
                .header("X-Tenant-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should work when tenant guard is disabled and no tenant mismatch")
    void tenantGuardDisabledShouldWorkNormally() throws Exception {
        // Given: Request with matching tenantId or no tenantId conflict
        String body = "{\"eventName\":\"pv\",\"sessionId\":\"s1\",\"tenantId\":9}";

        // When: Send request with tenant guard disabled
        // Then: Should succeed
        mockMvc.perform(post("/api/v1/events/collect")
                .header("X-Tenant-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }
}

/**
 * Test class for audit log interceptor disabled scenario.
 * When audit.enabled=false, audit log interceptor should not record logs.
 */
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TestPropertySource(properties = {
        "audit.enabled=false"
})
class ConfigBeansConditionAuditDisabledTest extends PostgresTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should work normally when audit is disabled")
    void auditLogInterceptorDisabledShouldWorkNormally() throws Exception {
        /**
         * When audit is disabled, requests should still work normally.
         * The difference is that audit logs won't be recorded.
         */
        String body = "{\"eventName\":\"pv\",\"sessionId\":\"s1\",\"tenantId\":1}";

        // When: Send request with audit disabled
        // Then: Should succeed (audit being disabled doesn't block requests)
        mockMvc.perform(post("/api/v1/events/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated());
    }
}

// Note: audit.enabled=true scenario is covered by functional tests that verify
// audit logs are actually recorded

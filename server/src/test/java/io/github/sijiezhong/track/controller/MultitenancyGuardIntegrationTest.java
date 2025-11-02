package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "tenant.guard.enabled=true"
})
public class MultitenancyGuardIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminApiShouldRejectMissingTenantHeader() throws Exception {
        String body = "{\"appKey\":\"k1\",\"appName\":\"n1\",\"tenantId\":1}";
        mockMvc.perform(post("/api/v1/admin/apps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminApiShouldRejectMismatchedTenant() throws Exception {
        String body = "{\"appKey\":\"k2\",\"appName\":\"n2\",\"tenantId\":2}";
        mockMvc.perform(post("/api/v1/admin/apps")
                        .header("X-Tenant-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminApiShouldAllowMatchedTenant() throws Exception {
        String body = "{\"appKey\":\"k3\",\"appName\":\"n3\",\"tenantId\":1}";
        mockMvc.perform(post("/api/v1/admin/apps")
                        .header("X-Tenant-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }
}



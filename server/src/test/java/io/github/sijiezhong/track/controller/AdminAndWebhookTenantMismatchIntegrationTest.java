package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class AdminAndWebhookTenantMismatchIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminCreateAppShould403OnTenantMismatch() throws Exception {
        String body = "{\"tenantId\":2,\"appKey\":\"k\",\"appName\":\"n\"}";
        mockMvc.perform(post("/api/v1/admin/apps")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void webhookCreateShould403OnTenantMismatch() throws Exception {
        String body = "{\"tenantId\":2,\"url\":\"https://example.com/h\",\"secret\":\"s\"}";
        mockMvc.perform(post("/api/v1/webhooks")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}



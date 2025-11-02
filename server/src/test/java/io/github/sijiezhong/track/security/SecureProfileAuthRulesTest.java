package io.github.sijiezhong.track.security;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ActiveProfiles("secure")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class SecureProfileAuthRulesTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void collectAllowedWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .param("eventName", "pv")
                        .param("sessionId", "s-secure-1")
                        .param("tenantId", "1"))
                .andExpect(status().isCreated());
    }

    @Test
    void actuatorHealthAllowedWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void analyticsRequiresAuthAndHstsOnHttps() throws Exception {
        // no auth -> 401
        mockMvc.perform(get("/api/v1/events/segmentation")
                        .header("X-Tenant-Id", 1)
                        .param("eventName", "pv")
                        .param("by", "browser"))
                .andExpect(status().isUnauthorized());

        // with JWT role ANALYST -> 200
        mockMvc.perform(get("/api/v1/events/segmentation")
                        .secure(true)
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ANALYST")
                        .param("eventName", "pv")
                        .param("by", "browser"))
                .andExpect(status().isOk())
                .andExpect(header().string("Strict-Transport-Security", org.hamcrest.Matchers.containsString("max-age")));
    }
}



package io.github.sijiezhong.track.config;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class WebhookGuardCoverageTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void replayLatestWithoutTenantHeaderShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/replay/latest")
                        .header("Authorization", "Bearer role:ADMIN"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void replayLatestWithTenantHeaderShouldBeOk() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/replay/latest")
                        .header("Authorization", "Bearer role:ADMIN")
                        .header("X-Tenant-Id", 1))
                .andExpect(status().isOk());
    }
}



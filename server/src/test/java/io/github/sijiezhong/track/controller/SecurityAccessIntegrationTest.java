package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("secure")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class SecurityAccessIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void analyticsWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/events/trend")
                        .header("X-Tenant-Id", 1)
                        .param("eventName", "page_view")
                        .param("interval", "daily")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void analyticsWithReadonlyShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/events/path")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:READONLY"))
                .andExpect(status().isForbidden());
    }

    @Test
    void analyticsWithAnalystShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/events/retention")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ANALYST")
                        .param("cohortEvent", "register")
                        .param("returnEvent", "login")
                        .param("day", "1"))
                .andExpect(status().isOk());
    }
}



package io.github.sijiezhong.track.observability;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@org.springframework.test.context.TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration"
})
public class ObservabilityIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointShouldBeUp() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void prometheusShouldExposeCustomEventCounter() throws Exception {
        // create one event so counter increments (use GET to avoid CSRF)
        mockMvc.perform(get("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .param("eventName", "pv")
                        .param("sessionId", "sess-obs-1")
                        .param("tenantId", "1"))
                .andExpect(status().isCreated());

        MvcResult res = mockMvc.perform(get("/actuator/metrics/events_created_total"))
                .andExpect(status().isOk())
                .andReturn();
        String text = res.getResponse().getContentAsString();
        Assertions.assertTrue(text.contains("events_created_total"));
    }
}



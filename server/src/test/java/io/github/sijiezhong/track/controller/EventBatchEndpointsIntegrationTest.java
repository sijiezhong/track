package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventBatchEndpointsIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void strictBatchShouldReturn400WhenAnyInvalid() throws Exception {
        String body = "[" +
                "{\"eventName\":\"pv\",\"sessionId\":\"sess-ok\",\"tenantId\":1}," +
                // invalid: missing eventName and sessionId
                "{\"tenantId\":1}" +
                "]";
        mockMvc.perform(post("/api/v1/events/collect/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void partialBatchShouldReturnPerItemStatuses() throws Exception {
        String body = "[" +
                // invalid one first (missing sessionId)
                "{\"eventName\":\"pv\",\"tenantId\":1}," +
                // valid one
                "{\"eventName\":\"click\",\"sessionId\":\"sess2\",\"tenantId\":1}" +
                "]";
        var res = mockMvc.perform(post("/api/v1/events/collect/batch/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String json = res.getResponse().getContentAsString();
        assertThat(json).contains("\"status\":\"failed\"");
        assertThat(json).contains("\"status\":\"created\"");
    }
}



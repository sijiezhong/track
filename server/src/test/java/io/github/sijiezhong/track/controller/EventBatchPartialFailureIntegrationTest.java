package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventBatchPartialFailureIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void batchResultShouldReturnPerItemStatus() throws Exception {
        String body = "[\n" +
                // valid
                "  {\"eventName\":\"pv\",\"sessionId\":\"sess-b1\",\"tenantId\":1},\n" +
                // invalid: missing eventName
                "  {\"sessionId\":\"sess-b2\",\"tenantId\":1},\n" +
                // valid
                "  {\"eventName\":\"click\",\"sessionId\":\"sess-b3\",\"tenantId\":1}\n" +
                "]";

        MvcResult res = mockMvc.perform(post("/api/v1/events/collect/batch/result")
                        .header("X-Tenant-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        String json = res.getResponse().getContentAsString();
        // 期待三个条目，索引0 created，索引1 failed，索引2 created
        Assertions.assertTrue(json.contains("\"index\":0"));
        Assertions.assertTrue(json.contains("\"status\":\"created\""));
        Assertions.assertTrue(json.contains("\"index\":1"));
        Assertions.assertTrue(json.contains("\"status\":\"failed\""));
        Assertions.assertTrue(json.contains("\"index\":2"));
    }
}



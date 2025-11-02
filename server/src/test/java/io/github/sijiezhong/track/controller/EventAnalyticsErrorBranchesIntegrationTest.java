package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventAnalyticsErrorBranchesIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void funnelShouldReturn400WhenStepsLessThanTwo() throws Exception {
        mockMvc.perform(get("/api/v1/events/funnel")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ANALYST")
                        .param("steps", "page_view"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void segmentationShouldReturn400OnInvalidBy() throws Exception {
        mockMvc.perform(get("/api/v1/events/segmentation")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ANALYST")
                        .param("eventName", "pv")
                        .param("by", "invalidKey"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void heatmapShouldReturn400OnInvalidBucket() throws Exception {
        mockMvc.perform(get("/api/v1/events/heatmap")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ANALYST")
                        .param("eventName", "click")
                        .param("bucket", "year"))
                .andExpect(status().isBadRequest());
    }
}



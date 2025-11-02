package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
public class AdvancedAnalyticsIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void segmentationByBrowserShouldReturnCounts() throws Exception {
        // seed minimal events via existing controller
        mockMvc.perform(get("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15")
                        .param("eventName", "pv").param("sessionId", "seg-1").param("tenantId", "1"))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .param("eventName", "pv").param("sessionId", "seg-2").param("tenantId", "1"))
                .andExpect(status().isCreated());

        MvcResult res = mockMvc.perform(get("/api/v1/events/segmentation")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ANALYST")
                        .param("eventName", "pv")
                        .param("by", "browser")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String json = res.getResponse().getContentAsString();
        Assertions.assertTrue(json.contains("items"));
    }

    @Test
    void heatmapByHourShouldReturnBuckets() throws Exception {
        mockMvc.perform(get("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .param("eventName", "click").param("sessionId", "hm-1").param("tenantId", "1"))
                .andExpect(status().isCreated());

        MvcResult res = mockMvc.perform(get("/api/v1/events/heatmap")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ANALYST")
                        .param("eventName", "click")
                        .param("bucket", "hour")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        Assertions.assertTrue(json.contains("buckets"));
    }
}



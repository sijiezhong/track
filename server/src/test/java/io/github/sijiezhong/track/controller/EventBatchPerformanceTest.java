package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Performance tests for batch event collection operations.
 * 
 * These tests verify that batch operations remain performant with large batches.
 * 
 * Note: These are not strict performance benchmarks, but rather smoke tests
 * to ensure operations complete within reasonable time.
 */
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventBatchPerformanceTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    private static final int LARGE_BATCH_SIZE = 500;

    private String buildBatchJson(int size) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(",");
            sb.append("{")
                    .append("\"eventName\":\"pv\",")
                    .append("\"sessionId\":\"sess-batch-").append(i).append("\",")
                    .append("\"tenantId\":1,")
                    .append("\"properties\":{\"index\":").append(i).append("}")
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    @Test
    @DisplayName("Strict batch should complete within reasonable time for large batch")
    void should_CompleteStrictBatch_WithinReasonableTime_ForLargeBatch() throws Exception {
        String body = buildBatchJson(LARGE_BATCH_SIZE);
        
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(post("/api/v1/events/collect/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Performance assertion: should complete within 10 seconds for 500 events
        assertThat(duration).isLessThan(10000);
        
        System.out.println("Strict batch for " + LARGE_BATCH_SIZE + " events completed in " + duration + "ms");
    }

    @Test
    @DisplayName("Batch with result should complete within reasonable time for large batch")
    void should_CompleteBatchWithResult_WithinReasonableTime_ForLargeBatch() throws Exception {
        String body = buildBatchJson(LARGE_BATCH_SIZE);
        
        long startTime = System.currentTimeMillis();
        
        var result = mockMvc.perform(post("/api/v1/events/collect/batch/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify response contains expected data
        String json = result.getResponse().getContentAsString();
        assertThat(json).contains("\"status\":\"created\"");
        
        // Performance assertion: should complete within 10 seconds for 500 events
        assertThat(duration).isLessThan(10000);
        
        System.out.println("Batch with result for " + LARGE_BATCH_SIZE + " events completed in " + duration + "ms");
    }

    @Test
    @DisplayName("Batch with mixed valid and invalid should complete within reasonable time")
    void should_CompleteMixedBatch_WithinReasonableTime() throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < LARGE_BATCH_SIZE; i++) {
            if (i > 0) sb.append(",");
            // Every 10th event is invalid (missing eventName)
            if (i % 10 == 0) {
                sb.append("{\"sessionId\":\"sess-invalid-").append(i).append("\"}");
            } else {
                sb.append("{")
                        .append("\"eventName\":\"pv\",")
                        .append("\"sessionId\":\"sess-valid-").append(i).append("\"")
                        .append("}");
            }
        }
        sb.append("]");
        String body = sb.toString();
        
        long startTime = System.currentTimeMillis();
        
        var result = mockMvc.perform(post("/api/v1/events/collect/batch/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify response contains both created and failed statuses
        String json = result.getResponse().getContentAsString();
        assertThat(json).contains("\"status\":\"created\"");
        assertThat(json).contains("\"status\":\"failed\"");
        
        // Performance assertion: should complete within 10 seconds
        assertThat(duration).isLessThan(10000);
        
        System.out.println("Mixed batch for " + LARGE_BATCH_SIZE + " events completed in " + duration + "ms");
    }
}


package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.repository.SessionRepository;
import io.github.sijiezhong.track.testsupport.EventTestBuilder;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import io.github.sijiezhong.track.testsupport.SessionTestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static io.github.sijiezhong.track.testsupport.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Performance tests for data export and batch operations.
 * 
 * These tests verify that operations remain performant with large datasets.
 * 
 * Note: These are not strict performance benchmarks, but rather smoke tests
 * to ensure operations complete within reasonable time.
 */
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventExportPerformanceTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private SessionRepository sessionRepository;

    private static final int LARGE_DATASET_SIZE = 1000;

    @BeforeEach
    void setupLargeDataset() {
        // PostgresTestBase.cleanupDatabase() is already called before this method
        
        // Create session for the large dataset
        Session s = SessionTestBuilder.create()
                .withSessionId("sess-perf-1")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        s = sessionRepository.save(s);

        // Create large dataset for performance testing
        LocalDateTime baseTime = FIXED_TIME;
        for (int i = 0; i < LARGE_DATASET_SIZE; i++) {
            Event event = EventTestBuilder.create()
                    .withEventName(EVENT_PAGE_VIEW)
                    .withTenantId(DEFAULT_TENANT_ID)
                    .withSessionId(s.getId())
                    .withEventTime(baseTime.plusSeconds(i))
                    .withProperties(DEFAULT_PROPERTIES)
                    .withUserId(DEFAULT_USER_ID)
                    .build();
            eventRepository.save(event);
        }
    }

    @Test
    @DisplayName("CSV export should complete within reasonable time for large dataset")
    void should_CompleteCsvExport_WithinReasonableTime_ForLargeDataset() throws Exception {
        long startTime = System.currentTimeMillis();
        
        var result = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", DEFAULT_TENANT_ID)
                        .accept("text/csv"))
                .andExpect(status().isOk())
                .andReturn();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify response contains expected data
        String csv = result.getResponse().getContentAsString();
        assertThat(csv).contains("eventName");
        assertThat(csv.split("\n").length).isGreaterThan(LARGE_DATASET_SIZE); // Header + data rows
        
        // Performance assertion: should complete within 5 seconds for 1000 records
        // This is a smoke test, not a strict benchmark
        assertThat(duration).isLessThan(5000);
        
        System.out.println("CSV export for " + LARGE_DATASET_SIZE + " records completed in " + duration + "ms");
    }

    @Test
    @DisplayName("CSV export with filtering should complete within reasonable time")
    void should_CompleteFilteredCsvExport_WithinReasonableTime() throws Exception {
        long startTime = System.currentTimeMillis();
        
        var result = mockMvc.perform(get("/api/v1/events/export.csv")
                        .header("X-Tenant-Id", DEFAULT_TENANT_ID)
                        .param("eventName", EVENT_PAGE_VIEW)
                        .accept("text/csv"))
                .andExpect(status().isOk())
                .andReturn();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify response contains expected data
        String csv = result.getResponse().getContentAsString();
        assertThat(csv).contains(EVENT_PAGE_VIEW);
        
        // Performance assertion: should complete within 5 seconds
        assertThat(duration).isLessThan(5000);
        
        System.out.println("Filtered CSV export completed in " + duration + "ms");
    }

    @Test
    @DisplayName("Parquet export should complete within reasonable time for large dataset")
    void should_CompleteParquetExport_WithinReasonableTime_ForLargeDataset() throws Exception {
        long startTime = System.currentTimeMillis();
        
        var result = mockMvc.perform(get("/api/v1/events/export.parquet")
                        .header("X-Tenant-Id", DEFAULT_TENANT_ID))
                .andExpect(status().isOk())
                .andReturn();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify response is not empty
        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content.length).isGreaterThan(0);
        
        // Performance assertion: should complete within 5 seconds for 1000 records
        assertThat(duration).isLessThan(5000);
        
        System.out.println("Parquet export for " + LARGE_DATASET_SIZE + " records completed in " + duration + "ms");
    }
}


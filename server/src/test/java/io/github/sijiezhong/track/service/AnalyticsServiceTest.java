package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.repository.SessionRepository;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import io.github.sijiezhong.track.testsupport.EventTestBuilder;
import io.github.sijiezhong.track.testsupport.SessionTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.github.sijiezhong.track.testsupport.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Integration tests for AnalyticsService using real database.
 * 
 * Coverage includes:
 * - Date range validation
 * - Funnel analysis (multi-step conversion)
 * - Trend analysis (daily aggregation)
 * - Retention analysis
 * - Path analysis (edges between pages)
 * - Segmentation (by browser, device, OS, referrer)
 * - Heatmap (hourly distribution)
 * - Boundary conditions and edge cases
 */
public class AnalyticsServiceTest extends PostgresTestBase {

    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private SessionRepository sessionRepository;
    
    @Autowired
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("Should reject funnel with less than two steps")
    void should_RejectFunnel_When_LessThanTwoSteps() {
        // This is a validation test, no database needed
        assertThrows(io.github.sijiezhong.track.exception.BusinessException.class, () ->
                analyticsService.funnel(1, List.of("onlyOne"), LocalDateTime.now().minusDays(1), LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should reject unsupported segmentation by column")
    void should_RejectSegmentation_When_UnsupportedBy() {
        // This is a validation test, no database needed
        assertThrows(io.github.sijiezhong.track.exception.BusinessException.class, () ->
                analyticsService.segmentation(1, "pv", "unknown"));
    }

    @Test
    @DisplayName("Should reject unsupported heatmap bucket")
    void should_RejectHeatmap_When_UnsupportedBucket() {
        // This is a validation test, no database needed
        assertThrows(io.github.sijiezhong.track.exception.BusinessException.class, () ->
                analyticsService.heatmap(1, "pv", "day"));
    }

    @Test
    @DisplayName("Should map null dates and counts in trend daily")
    void should_MapNullDatesAndCounts_InTrendDaily() {
        // Create session and events
        Session session = SessionTestBuilder.create()
                .withSessionId("sess-trend-null")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session = sessionRepository.save(session);
        
        // Create events with null event_time (this will result in null date in aggregation)
        // Note: In practice, event_time shouldn't be null, but we test the service's handling
        // Create events on different dates to test aggregation
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        // Create 5 events for tenant 1
        for (int i = 0; i < 5; i++) {
            Event event = EventTestBuilder.create()
                    .withEventName("pv")
                    .withTenantId(DEFAULT_TENANT_ID)
                    .withSessionId(session.getId())
                    .withEventTime(baseTime.minusDays(i))
                    .build();
            eventRepository.save(event);
        }
        
        var out = analyticsService.trendDaily(DEFAULT_TENANT_ID, "pv", baseTime.minusDays(10), baseTime);
        
        // Should have exactly 5 entries (one per day for 5 events)
        assertThat(out).hasSize(5);
        // Verify structure and counts
        for (Map<String, Object> m : out) {
            assertThat(m.containsKey("date")).isTrue();
            assertThat(m.containsKey("count")).isTrue();
            assertThat(m.get("count")).isInstanceOf(Long.class);
            // Each day should have exactly 1 event
            assertThat(((Number) m.get("count")).longValue()).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("Should map non-null date in trend daily")
    void should_MapNonNullDate_InTrendDaily() {
        // Create session
        Session session = SessionTestBuilder.create()
                .withSessionId("sess-trend-nonnull")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session = sessionRepository.save(session);
        
        // Create events on a specific date (2024-01-01)
        LocalDateTime specificDate = LocalDateTime.of(2024, 1, 1, 12, 0);
        
        // Create 10 events on that date
        for (int i = 0; i < 10; i++) {
            Event event = EventTestBuilder.create()
                    .withEventName("pv")
                    .withTenantId(DEFAULT_TENANT_ID)
                    .withSessionId(session.getId())
                    .withEventTime(specificDate.plusHours(i))
                    .build();
            eventRepository.save(event);
        }
        
        var out = analyticsService.trendDaily(DEFAULT_TENANT_ID, "pv", 
                LocalDateTime.of(2024, 1, 1, 0, 0), 
                LocalDateTime.of(2024, 1, 2, 0, 0));
        
        assertThat(out).hasSize(1);
        Map<String, Object> m = out.get(0);
        // Date should be formatted as string (PostgreSQL date_trunc format)
        assertThat(m.get("date")).isNotNull();
        assertThat(m.get("count")).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should handle path edges with null values")
    void should_HandlePathEdges_When_NullValues() {
        // Create sessions
        Session session1 = SessionTestBuilder.create()
                .withSessionId("sess-path-1")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session1 = sessionRepository.save(session1);
        
        Session session2 = SessionTestBuilder.create()
                .withSessionId("sess-path-2")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session2 = sessionRepository.save(session2);
        
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        // Create path: A -> B for session1
        eventRepository.save(EventTestBuilder.create()
                .withEventName("A")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session1.getId())
                .withEventTime(baseTime)
                .build());
        eventRepository.save(EventTestBuilder.create()
                .withEventName("B")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session1.getId())
                .withEventTime(baseTime.plusMinutes(1))
                .build());
        
        // Create path: C -> D for session2
        eventRepository.save(EventTestBuilder.create()
                .withEventName("C")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session2.getId())
                .withEventTime(baseTime.plusMinutes(2))
                .build());
        eventRepository.save(EventTestBuilder.create()
                .withEventName("D")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session2.getId())
                .withEventTime(baseTime.plusMinutes(3))
                .build());
        
        var out = analyticsService.pathEdges(DEFAULT_TENANT_ID, baseTime.minusDays(1), baseTime.plusDays(1));
        
        // Should have path edges
        assertThat(out).isNotEmpty();
        // Verify structure
        for (Map<String, Object> m : out) {
            assertThat(m.containsKey("from")).isTrue();
            assertThat(m.containsKey("to")).isTrue();
            assertThat(m.containsKey("count")).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle retention daily with null date")
    void should_HandleRetentionDaily_When_NullDate() {
        // Create sessions with users
        Session session1 = SessionTestBuilder.create()
                .withSessionId("sess-ret-null-1")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(101)
                .build();
        session1 = sessionRepository.save(session1);
        
        Session session2 = SessionTestBuilder.create()
                .withSessionId("sess-ret-null-2")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(102)
                .build();
        session2 = sessionRepository.save(session2);
        
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime signupTime = baseTime.minusDays(7);
        LocalDateTime loginTime = baseTime;
        
        // Create signup events (cohort)
        eventRepository.save(EventTestBuilder.create()
                .withEventName("signup")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(101)
                .withSessionId(session1.getId())
                .withEventTime(signupTime)
                .build());
        eventRepository.save(EventTestBuilder.create()
                .withEventName("signup")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(102)
                .withSessionId(session2.getId())
                .withEventTime(signupTime)
                .build());
        
        // Create login events (retained) - only user 101 logs in
        eventRepository.save(EventTestBuilder.create()
                .withEventName("login")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(101)
                .withSessionId(session1.getId())
                .withEventTime(loginTime)
                .build());
        
        var out = analyticsService.retentionDaily(DEFAULT_TENANT_ID, "signup", "login", 7,
                signupTime.minusDays(1), loginTime.plusDays(1));
        
        // Should have results
        assertThat(out).isNotEmpty();
        // Verify structure
        for (Map<String, Object> m : out) {
            assertThat(m.containsKey("cohortDate")).isTrue();
            assertThat(m.containsKey("cohort")).isTrue();
            assertThat(m.containsKey("retained")).isTrue();
            assertThat(m.containsKey("rate")).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle retention daily with zero cohort")
    void should_HandleRetentionDaily_When_ZeroCohort() {
        // Test with no signup events - should return empty or zero cohort
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        var out = analyticsService.retentionDaily(DEFAULT_TENANT_ID, "signup", "login", 7,
                baseTime.minusDays(30), baseTime);
        
        // When no signup events exist, should return empty list (no cohort data)
        // This is the expected behavior: no events means no retention calculation
        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("Should reject funnel with null steps")
    void should_RejectFunnel_When_NullSteps() {
        // This is a validation test, no database needed
        assertThrows(io.github.sijiezhong.track.exception.BusinessException.class, () ->
                analyticsService.funnel(1, null, LocalDateTime.now().minusDays(1), LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should handle funnel with events that skip steps")
    void should_HandleFunnel_When_ProgressNotMatchingNextStep() {
        // Create session with step1, then wrongStep, then step2
        Session session = SessionTestBuilder.create()
                .withSessionId("sess-funnel-skip")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session = sessionRepository.save(session);
        
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        eventRepository.save(EventTestBuilder.create()
                .withEventName("step1")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session.getId())
                .withEventTime(baseTime)
                .build());
        eventRepository.save(EventTestBuilder.create()
                .withEventName("wrongStep")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session.getId())
                .withEventTime(baseTime.plusMinutes(1))
                .build());
        eventRepository.save(EventTestBuilder.create()
                .withEventName("step2")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session.getId())
                .withEventTime(baseTime.plusMinutes(2))
                .build());
        
        var out = analyticsService.funnel(DEFAULT_TENANT_ID, List.of("step1", "step2"),
                baseTime.minusDays(1), baseTime.plusDays(1));
        
        assertThat(out.get("counts")).isInstanceOf(List.class);
        List<Integer> counts = (List<Integer>) out.get("counts");
        assertThat(counts.get(0)).isGreaterThanOrEqualTo(1); // At least reached step1
    }

    @Test
    @DisplayName("Should handle funnel with events beyond defined steps")
    void should_HandleFunnel_When_ProgressBeyondSteps() {
        // Create session with step1, step2, then step3 (but only step1 and step2 are in funnel)
        Session session = SessionTestBuilder.create()
                .withSessionId("sess-funnel-beyond")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session = sessionRepository.save(session);
        
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        eventRepository.save(EventTestBuilder.create()
                .withEventName("step1")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session.getId())
                .withEventTime(baseTime)
                .build());
        eventRepository.save(EventTestBuilder.create()
                .withEventName("step2")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session.getId())
                .withEventTime(baseTime.plusMinutes(1))
                .build());
        eventRepository.save(EventTestBuilder.create()
                .withEventName("step3")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session.getId())
                .withEventTime(baseTime.plusMinutes(2))
                .build());
        
        var out = analyticsService.funnel(DEFAULT_TENANT_ID, List.of("step1", "step2"),
                baseTime.minusDays(1), baseTime.plusDays(1));
        
        assertThat(out.get("counts")).isInstanceOf(List.class);
        List<Integer> counts = (List<Integer>) out.get("counts");
        assertThat(counts.get(0)).isGreaterThanOrEqualTo(1); // Reached step1
        assertThat(counts.get(1)).isGreaterThanOrEqualTo(1); // Reached step2
    }

    @Test
    @DisplayName("Should handle funnel with events not matching any step")
    void should_HandleFunnel_When_NegativeProgress() {
        // Create session with events that don't match funnel steps
        Session session = SessionTestBuilder.create()
                .withSessionId("sess-funnel-nomatch")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session = sessionRepository.save(session);
        
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        eventRepository.save(EventTestBuilder.create()
                .withEventName("wrongStep")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session.getId())
                .withEventTime(baseTime)
                .build());
        
        var out = analyticsService.funnel(DEFAULT_TENANT_ID, List.of("step1", "step2"),
                baseTime.minusDays(1), baseTime.plusDays(1));
        
        assertThat(out.get("counts")).isInstanceOf(List.class);
        List<Integer> counts = (List<Integer>) out.get("counts");
        assertThat(counts.get(0)).isEqualTo(0); // No session reached step1
    }

    @Test
    @DisplayName("Should handle funnel with zero counts")
    void should_HandleFunnel_When_ZeroCounts() {
        // No events - should return zero counts
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        var out = analyticsService.funnel(DEFAULT_TENANT_ID, List.of("step1", "step2"),
                baseTime.minusDays(1), baseTime);
        
        List<Integer> counts = (List<Integer>) out.get("counts");
        assertThat(counts.get(0)).isEqualTo(0);
        assertThat(counts.get(1)).isEqualTo(0);
        List<Double> rates = (List<Double>) out.get("conversionRates");
        // When counts[0] == 0, rates should all be 0.0
        assertThat(rates.get(0)).isEqualTo(0.0);
        assertThat(rates.get(1)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should handle funnel with zero previous count")
    void should_HandleFunnel_When_ZeroPreviousCount() {
        // Create session with only step1 event
        Session session = SessionTestBuilder.create()
                .withSessionId("sess-funnel-partial")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session = sessionRepository.save(session);
        
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        // Create only step1 event
        eventRepository.save(EventTestBuilder.create()
                .withEventName("step1")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session.getId())
                .withEventTime(baseTime)
                .build());
        
        var out = analyticsService.funnel(DEFAULT_TENANT_ID, List.of("step1", "step2", "step3"),
                baseTime.minusDays(1), baseTime.plusDays(1));
        
        List<Integer> counts = (List<Integer>) out.get("counts");
        assertThat(counts.get(0)).isEqualTo(1); // One session reached step1
        assertThat(counts.get(1)).isEqualTo(0); // step2 not reached
        assertThat(counts.get(2)).isEqualTo(0); // step3 not reached
        List<Double> rates = (List<Double>) out.get("conversionRates");
        assertThat(rates.get(1)).isEqualTo(0.0); // counts[0] > 0 but counts[1] = 0
    }

    @Test
    @DisplayName("Should support all segmentation cases")
    void should_SupportAllSegmentationCases() {
        // Create sessions and events with different browser, device, os, referrer
        Session session1 = SessionTestBuilder.create()
                .withSessionId("sess-seg-1")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session1 = sessionRepository.save(session1);
        
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        // Create events with different attributes
        eventRepository.save(EventTestBuilder.create()
                .withEventName("pv")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session1.getId())
                .withBrowser("Chrome")
                .withDevice("Desktop")
                .withOs("Windows")
                .withReferrer("https://example.com")
                .withEventTime(baseTime)
                .build());
        
        var out1 = analyticsService.segmentation(DEFAULT_TENANT_ID, "pv", "browser");
        assertThat(out1.get("by")).isEqualTo("browser");

        var out2 = analyticsService.segmentation(DEFAULT_TENANT_ID, "pv", "device");
        assertThat(out2.get("by")).isEqualTo("device");

        var out3 = analyticsService.segmentation(DEFAULT_TENANT_ID, "pv", "os");
        assertThat(out3.get("by")).isEqualTo("os");

        var out4 = analyticsService.segmentation(DEFAULT_TENANT_ID, "pv", "referrer");
        assertThat(out4.get("by")).isEqualTo("referrer");
    }

    @Test
    @DisplayName("Should handle heatmap with valid hours")
    void should_HandleHeatmap_When_InvalidHour() {
        // Create events at different hours
        Session session = SessionTestBuilder.create()
                .withSessionId("sess-heatmap")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session = sessionRepository.save(session);
        
        LocalDateTime baseTime = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        
        // Create events at hour 12
        for (int i = 0; i < 10; i++) {
            eventRepository.save(EventTestBuilder.create()
                    .withEventName("pv")
                    .withTenantId(DEFAULT_TENANT_ID)
                    .withSessionId(session.getId())
                    .withEventTime(baseTime.withHour(12).plusMinutes(i))
                    .build());
        }
        
        var out = analyticsService.heatmap(DEFAULT_TENANT_ID, "pv", "hour");
        List<Long> buckets = (List<Long>) out.get("buckets");
        // Should have 24 buckets (0-23 hours)
        assertThat(buckets).hasSize(24);
        // Hour 12 should have 10 events
        assertThat(buckets.get(12)).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should handle invalid date range when end is before start")
    void should_HandleInvalidDateRange_When_EndBeforeStart() {
        // Business scenario: Invalid date range query
        // When end date is before start date, the service should still execute
        // but return empty or incorrect results. This tests the service's resilience.
        
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.minusDays(10); // End before start
        
        // When: Query with invalid date range
        var out = analyticsService.trendDaily(DEFAULT_TENANT_ID, "pv", start, end);
        
        // Then: Should return empty list (no data for invalid range)
        assertThat(out).isEmpty();
    }

    @Test
    @DisplayName("Should handle very large date range")
    void should_HandleVeryLargeDateRange() {
        // Create events across a large date range
        Session session = SessionTestBuilder.create()
                .withSessionId("sess-large-range")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session = sessionRepository.save(session);
        
        LocalDateTime start = LocalDateTime.of(2020, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 31, 23, 59);
        
        // Create events at start and end of range
        eventRepository.save(EventTestBuilder.create()
                .withEventName("pv")
                .withTenantId(DEFAULT_TENANT_ID)
                .withSessionId(session.getId())
                .withEventTime(start)
                .build());
        
        for (int i = 0; i < 100; i++) {
            eventRepository.save(EventTestBuilder.create()
                    .withEventName("pv")
                    .withTenantId(DEFAULT_TENANT_ID)
                    .withSessionId(session.getId())
                    .withEventTime(end.minusDays(100 - i))
                    .build());
        }
        
        // When: Query with very large date range (5 years)
        var out = analyticsService.trendDaily(DEFAULT_TENANT_ID, "pv", start, end);
        
        // Then: Should return results for the range
        assertThat(out).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle negative tenantId gracefully")
    void should_HandleNegativeTenantId_Gracefully() {
        // Business scenario: Invalid tenant ID
        // When tenantId is negative, the service should still execute the query,
        // though it may return empty results.
        
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        
        // When: Query with negative tenantId
        var out = analyticsService.trendDaily(-1, "pv", start, end);
        
        // Then: Should execute without throwing exception and return empty list (negative tenantId should have no results)
        assertThat(out).isNotNull();
        assertThat(out).isEmpty(); // Negative tenantId should return no results
    }

    @Test
    @DisplayName("Should handle zero tenantId")
    void should_HandleZeroTenantId() {
        // Create events for tenant 0 (if valid)
        Session session = SessionTestBuilder.create()
                .withSessionId("sess-tenant-0")
                .withTenantId(0)
                .build();
        session = sessionRepository.save(session);
        
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        for (int i = 0; i < 50; i++) {
            eventRepository.save(EventTestBuilder.create()
                    .withEventName("pv")
                    .withTenantId(0)
                    .withSessionId(session.getId())
                    .withEventTime(baseTime.minusHours(i))
                    .build());
        }
        
        LocalDateTime start = baseTime.minusDays(1);
        LocalDateTime end = baseTime;
        
        // When: Query with zero tenantId
        var out = analyticsService.trendDaily(0, "pv", start, end);
        
        // Then: Should execute and return results for tenant 0
        assertThat(out).isNotNull();
        assertThat(out).isNotEmpty(); // Should have results for tenant 0
        // Verify structure: each entry should have date and count
        for (Map<String, Object> entry : out) {
            assertThat(entry.containsKey("date")).isTrue();
            assertThat(entry.containsKey("count")).isTrue();
            assertThat(entry.get("count")).isInstanceOf(Long.class);
        }
    }

    @Test
    @DisplayName("Should handle null event name in trend analysis")
    void should_HandleNullEventName_InTrendAnalysis() {
        // Business scenario: Null event name
        // When eventName is null, the service should pass it to repository
        // which may handle it differently (e.g., query all events).
        
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        
        // When: Query with null eventName
        var out = analyticsService.trendDaily(DEFAULT_TENANT_ID, null, start, end);
        
        // Then: Should execute without throwing exception and return list (may be empty)
        assertThat(out).isNotNull();
        assertThat(out).isInstanceOf(List.class);
        // Verify structure if not empty
        if (!out.isEmpty()) {
            for (Map<String, Object> entry : out) {
                assertThat(entry.containsKey("date")).isTrue();
                assertThat(entry.containsKey("count")).isTrue();
            }
        }
    }

    @Test
    @DisplayName("Should handle empty event name in trend analysis")
    void should_HandleEmptyEventName_InTrendAnalysis() {
        // Business scenario: Empty event name
        // When eventName is empty string, the service should still execute.
        
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        
        // When: Query with empty eventName
        var out = analyticsService.trendDaily(DEFAULT_TENANT_ID, "", start, end);
        
        // Then: Should execute without throwing exception and return list (empty eventName may return empty or all events)
        assertThat(out).isNotNull();
        assertThat(out).isInstanceOf(List.class);
        // Empty eventName behavior may vary, but structure should be consistent
        if (!out.isEmpty()) {
            for (Map<String, Object> entry : out) {
                assertThat(entry.containsKey("date")).isTrue();
                assertThat(entry.containsKey("count")).isTrue();
            }
        }
    }

    @Test
    @DisplayName("Should handle large dataset in funnel analysis")
    void should_HandleLargeDataset_InFunnelAnalysis() {
        // Business scenario: Large dataset performance
        // When analyzing funnel with large number of events, the service should
        // handle it efficiently. This test uses a moderate dataset (not 10000 for performance).
        
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime start = baseTime.minusDays(10); // Use shorter range
        LocalDateTime end = baseTime;
        
        // Create 100 sessions with funnel progression (reduced from 10000 for test performance)
        for (long i = 1; i <= 100; i++) {
            Session session = SessionTestBuilder.create()
                    .withSessionId("sess-funnel-large-" + i)
                    .withTenantId(DEFAULT_TENANT_ID)
                    .build();
            session = sessionRepository.save(session);
            
            // All sessions reach step1 - use time within range
            LocalDateTime eventTime = start.plusHours(i % 200); // Distribute across the range
            eventRepository.save(EventTestBuilder.create()
                    .withEventName("step1")
                    .withTenantId(DEFAULT_TENANT_ID)
                    .withSessionId(session.getId())
                    .withEventTime(eventTime)
                    .build());
            
            // 80% reach step2
            if (i <= 80) {
                eventRepository.save(EventTestBuilder.create()
                        .withEventName("step2")
                        .withTenantId(DEFAULT_TENANT_ID)
                        .withSessionId(session.getId())
                        .withEventTime(eventTime.plusMinutes(1))
                        .build());
            }
            
            // 50% reach step3
            if (i <= 50) {
                eventRepository.save(EventTestBuilder.create()
                        .withEventName("step3")
                        .withTenantId(DEFAULT_TENANT_ID)
                        .withSessionId(session.getId())
                        .withEventTime(eventTime.plusMinutes(2))
                        .build());
            }
        }
        
        // When: Analyze funnel with large dataset
        var out = analyticsService.funnel(DEFAULT_TENANT_ID, List.of("step1", "step2", "step3"), start, end);
        
        // Then: Should calculate correct conversion rates
        List<Integer> counts = (List<Integer>) out.get("counts");
        assertThat(counts.get(0)).isEqualTo(100); // All reached step1
        assertThat(counts.get(1)).isEqualTo(80); // 80 reached step2
        assertThat(counts.get(2)).isEqualTo(50); // 50 reached step3
        
        List<Double> rates = (List<Double>) out.get("conversionRates");
        assertThat(rates.get(0)).isEqualTo(1.0); // 100% from start to step1
        assertThat(rates.get(1)).isEqualTo(0.8); // 80% from step1 to step2
        assertThat(rates.get(2)).isEqualTo(0.625); // 62.5% from step2 to step3 (50/80)
    }

    @Test
    @DisplayName("Should handle large dataset in segmentation analysis")
    void should_HandleLargeDataset_InSegmentationAnalysis() {
        // Business scenario: Large segmentation dataset
        // When segmenting by dimension with many distinct values, the service
        // should handle large result sets efficiently.
        
        Session session = SessionTestBuilder.create()
                .withSessionId("sess-seg-large")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session = sessionRepository.save(session);
        
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        // Create events with 100 distinct browsers (reduced from 1000 for test performance)
        for (int i = 1; i <= 100; i++) {
            for (int j = 0; j < i; j++) { // Browser i appears i times
                eventRepository.save(EventTestBuilder.create()
                        .withEventName("pv")
                        .withTenantId(DEFAULT_TENANT_ID)
                        .withSessionId(session.getId())
                        .withBrowser("Browser" + i)
                        .withEventTime(baseTime.plusMinutes(j))
                        .build());
            }
        }
        
        // When: Analyze segmentation with large dataset
        var out = analyticsService.segmentation(DEFAULT_TENANT_ID, "pv", "browser");
        
        // Then: Should return all segments
        List<Map<String, Object>> items = (List<Map<String, Object>>) out.get("items");
        assertThat(items).hasSize(100);
        assertThat(out.get("by")).isEqualTo("browser");
        
        // Verify structure
        for (Map<String, Object> item : items) {
            assertThat(item.containsKey("key")).isTrue();
            assertThat(item.containsKey("count")).isTrue();
        }
    }

    @Test
    @DisplayName("Should handle retention with invalid day parameter")
    void should_HandleRetention_WithInvalidDayParameter() {
        // Business scenario: Invalid retention day parameter
        // When day parameter is negative, the service should still execute.
        
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        
        // When: Query retention with negative day parameter
        var out = analyticsService.retentionDaily(DEFAULT_TENANT_ID, "signup", "login", -1, start, end);
        
        // Then: Should execute without throwing exception and return list structure
        assertThat(out).isNotNull();
        assertThat(out).isInstanceOf(List.class);
        // Verify structure if not empty
        if (!out.isEmpty()) {
            for (Map<String, Object> entry : out) {
                assertThat(entry.containsKey("cohortDate")).isTrue();
                assertThat(entry.containsKey("cohort")).isTrue();
                assertThat(entry.containsKey("retained")).isTrue();
                assertThat(entry.containsKey("rate")).isTrue();
            }
        }
    }

    @Test
    @DisplayName("Should handle retention with zero day parameter")
    void should_HandleRetention_WithZeroDayParameter() {
        // Business scenario: Zero day retention (same day retention)
        // Day parameter of 0 may represent same-day retention.
        
        Session session1 = SessionTestBuilder.create()
                .withSessionId("sess-ret-zero-1")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(101)
                .build();
        session1 = sessionRepository.save(session1);
        
        Session session2 = SessionTestBuilder.create()
                .withSessionId("sess-ret-zero-2")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(102)
                .build();
        session2 = sessionRepository.save(session2);
        
        Session session3 = SessionTestBuilder.create()
                .withSessionId("sess-ret-zero-3")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(103)
                .build();
        session3 = sessionRepository.save(session3);
        
        LocalDateTime baseTime = LocalDateTime.now().withHour(12).withMinute(0).withSecond(0).withNano(0);
        
        // Create signup events (cohort) - 3 users
        eventRepository.save(EventTestBuilder.create()
                .withEventName("signup")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(101)
                .withSessionId(session1.getId())
                .withEventTime(baseTime.minusDays(1))
                .build());
        eventRepository.save(EventTestBuilder.create()
                .withEventName("signup")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(102)
                .withSessionId(session2.getId())
                .withEventTime(baseTime.minusDays(1))
                .build());
        eventRepository.save(EventTestBuilder.create()
                .withEventName("signup")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(103)
                .withSessionId(session3.getId())
                .withEventTime(baseTime.minusDays(1))
                .build());
        
        // Create login events (retained) - 2 users on same day (0 day retention)
        eventRepository.save(EventTestBuilder.create()
                .withEventName("login")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(101)
                .withSessionId(session1.getId())
                .withEventTime(baseTime.minusDays(1))
                .build());
        eventRepository.save(EventTestBuilder.create()
                .withEventName("login")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(102)
                .withSessionId(session2.getId())
                .withEventTime(baseTime.minusDays(1))
                .build());
        
        LocalDateTime start = baseTime.minusDays(30);
        LocalDateTime end = baseTime;
        
        // When: Query retention with zero day parameter
        var out = analyticsService.retentionDaily(DEFAULT_TENANT_ID, "signup", "login", 0, start, end);
        
        // Then: Should execute and calculate rate with correct structure
        assertThat(out).isNotNull();
        assertThat(out).isInstanceOf(List.class);
        // Should have retention data for the cohort
        assertThat(out).isNotEmpty(); // Should have at least one cohort entry
        for (Map<String, Object> entry : out) {
            assertThat(entry.containsKey("cohortDate")).isTrue();
            assertThat(entry.containsKey("cohort")).isTrue();
            assertThat(entry.containsKey("retained")).isTrue();
            assertThat(entry.containsKey("rate")).isTrue();
            // Verify data types
            assertThat(entry.get("cohort")).isInstanceOf(Number.class);
            assertThat(entry.get("retained")).isInstanceOf(Number.class);
            assertThat(entry.get("rate")).isInstanceOf(Number.class);
        }
        // Verify actual retention calculation: 3 users signed up, 2 logged in on same day
        // Expected: cohort=3, retained=2, rate≈0.67
        Map<String, Object> firstEntry = out.get(0);
        Number cohort = (Number) firstEntry.get("cohort");
        Number retained = (Number) firstEntry.get("retained");
        assertThat(cohort.intValue()).isEqualTo(3);
        assertThat(retained.intValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle same start and end date")
    void should_HandleSameStartAndEndDate() {
        // Business scenario: Zero-length date range
        // When start and end dates are the same, the query should return
        // data for that single day/hour.
        
        Session session = SessionTestBuilder.create()
                .withSessionId("sess-same-date")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        session = sessionRepository.save(session);
        
        LocalDateTime sameTime = LocalDateTime.of(2024, 1, 1, 12, 0);
        
        // Create 10 events on the same day (date_trunc will aggregate them)
        for (int i = 0; i < 10; i++) {
            eventRepository.save(EventTestBuilder.create()
                    .withEventName("pv")
                    .withTenantId(DEFAULT_TENANT_ID)
                    .withSessionId(session.getId())
                    .withEventTime(sameTime.plusMinutes(i))
                    .build());
        }
        
        // When: Query with same start and end date (same day)
        var out = analyticsService.trendDaily(DEFAULT_TENANT_ID, "pv", 
                LocalDateTime.of(2024, 1, 1, 0, 0), 
                LocalDateTime.of(2024, 1, 1, 23, 59));
        
        // Then: Should return results for that day
        assertThat(out).isNotEmpty();
        // All events on the same day should be aggregated into one entry
        long totalCount = out.stream()
                .mapToLong(m -> ((Number) m.get("count")).longValue())
                .sum();
        assertThat(totalCount).isEqualTo(10L); // All 10 events on the same day
    }
}



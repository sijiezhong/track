package io.github.sijiezhong.track.repository;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;
import io.github.sijiezhong.track.testsupport.EventTestBuilder;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import io.github.sijiezhong.track.testsupport.SessionTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static io.github.sijiezhong.track.testsupport.TestConstants.DEFAULT_TENANT_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests for EventRepository custom query methods.
 * 
 * Coverage includes:
 * - findLatestByTenant
 * - aggregateDaily (daily aggregation)
 * - aggregatePathEdges (event path analysis)
 * - aggregateDailyRetention (retention analysis)
 * - findEventsForFunnel (funnel analysis)
 * - segmentCountByColumn (segmentation by browser, device, os, referrer)
 * - aggregateByHour (hourly aggregation)
 */
public class EventRepositoryTest extends PostgresTestBase {

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private SessionRepository sessionRepository;

  @Test
  @DisplayName("Should find latest event by tenant when events exist")
  void should_FindLatestByTenant_When_TenantHasEvents() {
    // Given: Create session and events with different times
    Session session = SessionTestBuilder.create()
        .withSessionId("sess-latest")
        .withTenantId(DEFAULT_TENANT_ID)
        .build();
    session = sessionRepository.save(session);

    LocalDateTime baseTime = LocalDateTime.now();

    Event event1 = EventTestBuilder.create()
        .withEventName("pv")
        .withTenantId(DEFAULT_TENANT_ID)
        .withSessionId(session.getId())
        .withEventTime(baseTime.minusHours(2))
        .build();
    eventRepository.save(event1);

    Event event2 = EventTestBuilder.create()
        .withEventName("click")
        .withTenantId(DEFAULT_TENANT_ID)
        .withSessionId(session.getId())
        .withEventTime(baseTime.minusHours(1))
        .build();
    eventRepository.save(event2);

    Event latest = EventTestBuilder.create()
        .withEventName("submit")
        .withTenantId(DEFAULT_TENANT_ID)
        .withSessionId(session.getId())
        .withEventTime(baseTime)
        .build();
    eventRepository.save(latest);

    // When: Find latest event for tenant
    List<Event> found = eventRepository.findLatestByTenant(DEFAULT_TENANT_ID);

    // Then: Should return the latest event
    assertThat(found).hasSize(1);
    assertThat(found.get(0).getId()).isEqualTo(latest.getId());
    assertThat(found.get(0).getEventName()).isEqualTo("submit");
  }

  @Test
  @DisplayName("Should aggregate daily events correctly")
  void should_AggregateDaily_When_ValidParameters() {
    // Given: Create session and events on different days
    Session session = SessionTestBuilder.create()
        .withSessionId("sess-daily")
        .withTenantId(DEFAULT_TENANT_ID)
        .build();
    session = sessionRepository.save(session);

    LocalDateTime day1 = LocalDateTime.of(2024, 1, 1, 12, 0);
    LocalDateTime day2 = LocalDateTime.of(2024, 1, 2, 14, 0);
    LocalDateTime day3 = LocalDateTime.of(2024, 1, 3, 16, 0);

    // Create 3 events on day1
    for (int i = 0; i < 3; i++) {
      eventRepository.save(EventTestBuilder.create()
          .withEventName("pv")
          .withTenantId(DEFAULT_TENANT_ID)
          .withSessionId(session.getId())
          .withEventTime(day1.plusHours(i))
          .build());
    }

    // Create 2 events on day2
    for (int i = 0; i < 2; i++) {
      eventRepository.save(EventTestBuilder.create()
          .withEventName("pv")
          .withTenantId(DEFAULT_TENANT_ID)
          .withSessionId(session.getId())
          .withEventTime(day2.plusHours(i))
          .build());
    }

    // Create 1 event on day3
    eventRepository.save(EventTestBuilder.create()
        .withEventName("pv")
        .withTenantId(DEFAULT_TENANT_ID)
        .withSessionId(session.getId())
        .withEventTime(day3)
        .build());

    // When: Aggregate daily
    List<Object[]> results = eventRepository.aggregateDaily(
        DEFAULT_TENANT_ID, "pv",
        LocalDateTime.of(2024, 1, 1, 0, 0),
        LocalDateTime.of(2024, 1, 3, 23, 59));

    // Then: Should have 3 days with correct counts
    assertThat(results).hasSize(3);
    // Results are Object[]: [date, count] - PostgreSQL returns BigInteger for
    // count(*) but Hibernate may map to Long
    Number count0 = (Number) results.get(0)[1];
    Number count1 = (Number) results.get(1)[1];
    Number count2 = (Number) results.get(2)[1];
    assertThat(count0.longValue()).isEqualTo(3L);
    assertThat(count1.longValue()).isEqualTo(2L);
    assertThat(count2.longValue()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Should aggregate path edges correctly")
  void should_AggregatePathEdges_When_ValidParameters() {
    // Given: Create sessions with event paths
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

    LocalDateTime baseTime = LocalDateTime.now();

    // session1: A -> B -> C
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
    eventRepository.save(EventTestBuilder.create()
        .withEventName("C")
        .withTenantId(DEFAULT_TENANT_ID)
        .withSessionId(session1.getId())
        .withEventTime(baseTime.plusMinutes(2))
        .build());

    // session2: A -> B (same path, should be aggregated)
    eventRepository.save(EventTestBuilder.create()
        .withEventName("A")
        .withTenantId(DEFAULT_TENANT_ID)
        .withSessionId(session2.getId())
        .withEventTime(baseTime)
        .build());
    eventRepository.save(EventTestBuilder.create()
        .withEventName("B")
        .withTenantId(DEFAULT_TENANT_ID)
        .withSessionId(session2.getId())
        .withEventTime(baseTime.plusMinutes(1))
        .build());

    // When: Aggregate path edges
    List<Object[]> results = eventRepository.aggregatePathEdges(
        DEFAULT_TENANT_ID,
        baseTime.minusDays(1),
        baseTime.plusDays(1));

    // Then: Should have edges A->B (count=2), B->C (count=1)
    assertThat(results).hasSize(2);
    // Extract and verify exact results using extracting with tuple matching
    assertThat(results)
        .extracting(
            row -> (String) row[0], // from
            row -> (String) row[1], // to
            row -> ((Number) row[2]).longValue() // count
        )
        .containsExactlyInAnyOrder(
            tuple("A", "B", 2L),
            tuple("B", "C", 1L));
  }

  @Test
  @DisplayName("Should aggregate daily retention correctly")
  void should_AggregateDailyRetention_When_ValidParameters() {
    // Given: Create users with signup and login events
    Session session1 = SessionTestBuilder.create()
        .withSessionId("sess-ret-1")
        .withTenantId(DEFAULT_TENANT_ID)
        .withUserId(101)
        .build();
    session1 = sessionRepository.save(session1);

    Session session2 = SessionTestBuilder.create()
        .withSessionId("sess-ret-2")
        .withTenantId(DEFAULT_TENANT_ID)
        .withUserId(102)
        .build();
    session2 = sessionRepository.save(session2);

    LocalDateTime signupTime = LocalDateTime.of(2024, 1, 1, 10, 0);
    LocalDateTime loginTime = LocalDateTime.of(2024, 1, 8, 10, 0); // 7 days later

    // User 101: signup and login (7 days later)
    eventRepository.save(EventTestBuilder.create()
        .withEventName("signup")
        .withTenantId(DEFAULT_TENANT_ID)
        .withUserId(101)
        .withSessionId(session1.getId())
        .withEventTime(signupTime)
        .build());
    eventRepository.save(EventTestBuilder.create()
        .withEventName("login")
        .withTenantId(DEFAULT_TENANT_ID)
        .withUserId(101)
        .withSessionId(session1.getId())
        .withEventTime(loginTime)
        .build());

    // User 102: signup only (no login)
    eventRepository.save(EventTestBuilder.create()
        .withEventName("signup")
        .withTenantId(DEFAULT_TENANT_ID)
        .withUserId(102)
        .withSessionId(session2.getId())
        .withEventTime(signupTime)
        .build());

    // When: Aggregate daily retention (7 day retention)
    List<Object[]> results = eventRepository.aggregateDailyRetention(
        DEFAULT_TENANT_ID,
        "signup",
        "login",
        7,
        signupTime.minusDays(1),
        loginTime.plusDays(1));

    // Then: Should have retention data
    assertThat(results).isNotEmpty();
    // Results: [cohort_day, cohort, retained]
    // Cohort should be 2, retained should be 1
    for (Object[] row : results) {
      Number cohort = (Number) row[1];
      Number retained = (Number) row[2];
      assertThat(cohort.longValue()).isEqualTo(2L);
      assertThat(retained.longValue()).isEqualTo(1L);
    }
  }

  @Test
  @DisplayName("Should find events for funnel correctly")
  void should_FindEventsForFunnel_When_ValidParameters() {
    // Given: Create session with funnel steps
    Session session = SessionTestBuilder.create()
        .withSessionId("sess-funnel")
        .withTenantId(DEFAULT_TENANT_ID)
        .build();
    session = sessionRepository.save(session);

    LocalDateTime baseTime = LocalDateTime.now();

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

    // When: Find events for funnel
    String[] steps = { "step1", "step2", "step3" };
    List<Object[]> results = eventRepository.findEventsForFunnel(
        DEFAULT_TENANT_ID,
        steps,
        baseTime.minusDays(1),
        baseTime.plusDays(1));

    // Then: Should return all 3 events in order
    assertThat(results).hasSize(3);
    assertThat(results.get(0)[1]).isEqualTo("step1");
    assertThat(results.get(1)[1]).isEqualTo("step2");
    assertThat(results.get(2)[1]).isEqualTo("step3");
  }

  @Test
  @DisplayName("Should segment by browser correctly")
  void should_SegmentCountByColumn_When_BrowserColumn() {
    // Given: Create events with different browsers
    Session session = SessionTestBuilder.create()
        .withSessionId("sess-seg-browser")
        .withTenantId(DEFAULT_TENANT_ID)
        .build();
    session = sessionRepository.save(session);

    eventRepository.save(EventTestBuilder.create()
        .withEventName("pv")
        .withTenantId(DEFAULT_TENANT_ID)
        .withSessionId(session.getId())
        .withBrowser("Chrome")
        .build());

    eventRepository.save(EventTestBuilder.create()
        .withEventName("pv")
        .withTenantId(DEFAULT_TENANT_ID)
        .withSessionId(session.getId())
        .withBrowser("Chrome")
        .build());

    eventRepository.save(EventTestBuilder.create()
        .withEventName("pv")
        .withTenantId(DEFAULT_TENANT_ID)
        .withSessionId(session.getId())
        .withBrowser("Firefox")
        .build());

    // When: Segment by browser
    List<Object[]> results = eventRepository.segmentCountByColumn(
        DEFAULT_TENANT_ID, "pv", "browser");

    // Then: Should have Chrome=2, Firefox=1
    assertThat(results).hasSize(2);
    // Extract and verify exact results using extracting with tuple matching
    assertThat(results)
        .extracting(
            row -> (String) row[0], // key (browser name)
            row -> ((Number) row[1]).longValue() // count
        )
        .containsExactlyInAnyOrder(
            tuple("Chrome", 2L),
            tuple("Firefox", 1L));
  }

  @Test
  @DisplayName("Should segment by device correctly")
  void should_SegmentCountByColumn_When_DeviceColumn() {
    // Given: Create events with different devices
    Session session = SessionTestBuilder.create()
        .withSessionId("sess-seg-device")
        .withTenantId(DEFAULT_TENANT_ID)
        .build();
    session = sessionRepository.save(session);

    eventRepository.save(EventTestBuilder.create()
        .withEventName("pv")
        .withTenantId(DEFAULT_TENANT_ID)
        .withSessionId(session.getId())
        .withDevice("Desktop")
        .build());

    eventRepository.save(EventTestBuilder.create()
        .withEventName("pv")
        .withTenantId(DEFAULT_TENANT_ID)
        .withSessionId(session.getId())
        .withDevice("Mobile")
        .build());

    // When: Segment by device
    List<Object[]> results = eventRepository.segmentCountByColumn(
        DEFAULT_TENANT_ID, "pv", "device");

    // Then: Should have Desktop=1, Mobile=1
    assertThat(results).hasSize(2);
  }

  @Test
  @DisplayName("Should aggregate by hour correctly")
  void should_AggregateByHour_When_ValidParameters() {
    // Given: Create events at different hours
    Session session = SessionTestBuilder.create()
        .withSessionId("sess-hour")
        .withTenantId(DEFAULT_TENANT_ID)
        .build();
    session = sessionRepository.save(session);

    LocalDateTime baseTime = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

    // Create 3 events at hour 10
    for (int i = 0; i < 3; i++) {
      eventRepository.save(EventTestBuilder.create()
          .withEventName("pv")
          .withTenantId(DEFAULT_TENANT_ID)
          .withSessionId(session.getId())
          .withEventTime(baseTime.withHour(10).plusMinutes(i))
          .build());
    }

    // Create 2 events at hour 14
    for (int i = 0; i < 2; i++) {
      eventRepository.save(EventTestBuilder.create()
          .withEventName("pv")
          .withTenantId(DEFAULT_TENANT_ID)
          .withSessionId(session.getId())
          .withEventTime(baseTime.withHour(14).plusMinutes(i))
          .build());
    }

    // When: Aggregate by hour
    List<Object[]> results = eventRepository.aggregateByHour(
        DEFAULT_TENANT_ID, "pv");

    // Then: Should have hour 10 with count 3, hour 14 with count 2
    assertThat(results).isNotEmpty();
    boolean foundHour10 = false;
    boolean foundHour14 = false;
    for (Object[] row : results) {
      Integer hour = (Integer) row[0];
      Number count = (Number) row[1];
      if (hour == 10) {
        assertThat(count.longValue()).isEqualTo(3L);
        foundHour10 = true;
      }
      if (hour == 14) {
        assertThat(count.longValue()).isEqualTo(2L);
        foundHour14 = true;
      }
    }
    assertThat(foundHour10).isTrue();
    assertThat(foundHour14).isTrue();
  }
}

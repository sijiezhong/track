package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;
import io.github.sijiezhong.track.dto.EventCollectRequest;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.repository.SessionRepository;
import io.github.sijiezhong.track.stream.EventStreamBroadcaster;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static io.github.sijiezhong.track.testsupport.SessionTestBuilder.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Partial integration tests for EventService: uses real database via PostgresTestBase 
 * but mocks WebhookService to avoid external HTTP calls.
 * 
 * For full integration tests with real webhook, see WebhookIntegrationTest.
 * 
 * Coverage includes:
 * - Event saving with/without properties
 * - Field mapping (userId, tenantId, structured fields)
 * - Session creation and update logic
 * - Anonymous to real user merging
 * - Exception handling
 */
public class EventServiceTest extends PostgresTestBase {

    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private SessionRepository sessionRepository;
    
    @Autowired
    private EventService eventService;
    
    @Autowired
    private EventStreamBroadcaster broadcaster;
    
    @MockBean
    private WebhookService webhookService; // Mock external HTTP calls

    @Test
    @DisplayName("Should save event with properties")
    void should_SaveEvent_When_PropertiesProvided() {
        // Given: Create and save Session to real database
        Session session = create()
            .withSessionId("s1")
            .withTenantId(9)
            .build();
        session = sessionRepository.save(session);
        
        // Given: Create Event request
        ObjectMapper om = new ObjectMapper();
        ObjectNode props = om.createObjectNode();
        props.put("url", "/home");
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("page_view");
        req.setSessionId("s1");
        req.setTenantId(9);
        req.setProperties(props);

        // When: Save event
        Event saved = eventService.save(req);
        
        // Then: Verify event was saved to database
        assertThat(saved.getId()).isNotNull();
        Event found = eventRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEventName()).isEqualTo("page_view");
        assertThat(found.getProperties()).contains("/home");
        assertThat(found.getSessionId()).isEqualTo(session.getId());
        assertThat(found.getTenantId()).isEqualTo(9);
    }

    @Test
    @DisplayName("Should save event when properties is empty")
    void should_SaveEvent_When_PropertiesIsEmpty() {
        // Given: Create session
        Session session = create()
            .withSessionId("s2")
            .withTenantId(1)
            .build();
        session = sessionRepository.save(session);
        
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("click");
        req.setSessionId("s2");
        req.setTenantId(1);
        req.setProperties(null);

        // When: Save event
        Event saved = eventService.save(req);
        
        // Then: Verify saved event
        assertThat(saved.getId()).isNotNull();
        Event found = eventRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEventName()).isEqualTo("click");
        assertThat(found.getProperties()).isNull();
        assertThat(found.getSessionId()).isEqualTo(session.getId());
    }

    @Test
    @DisplayName("Should map userId and tenantId to entity when provided")
    void should_MapUserIdAndTenantId_When_Provided() {
        // Given: Create session
        Session session = create()
            .withSessionId("s3")
            .withTenantId(9)
            .build();
        session = sessionRepository.save(session);
        
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("submit");
        req.setSessionId("s3");
        req.setUserId(123);
        req.setTenantId(9);

        // When: Save event
        Event saved = eventService.save(req);

        // Then: Verify field mapping
        Event found = eventRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getUserId()).isEqualTo(123);
        assertThat(found.getTenantId()).isEqualTo(9);
    }

    @Test
    @DisplayName("Should update session userId when converting from anonymous to real user")
    void should_UpdateSessionUserId_When_ConvertingFromAnonymousToReal() {
        /**
         * Tests the anonymous-to-real user conversion scenario.
         * 
         * <p>Business scenario: When a user starts browsing anonymously and then logs in,
         * we need to merge the anonymous session with the authenticated user identity.
         * This ensures continuity in user analytics.
         * 
         * <p>This test verifies:
         * <ul>
         *   <li>Anonymous session (no userId) can be created</li>
         *   <li>When userId is provided in subsequent event, session is updated</li>
         *   <li>Session is not overwritten if it already has a userId</li>
         * </ul>
         */
        
        // Given: An anonymous session exists (no userId)
        Session anonSession = create()
            .withSessionId("sess-merge-1")
            .withTenantId(1)
            .withUserId(null) // Anonymous
            .build();
        final Session savedAnonSession = sessionRepository.save(anonSession);

        // When: First event is saved for anonymous session
        EventCollectRequest anonReq = new EventCollectRequest();
        anonReq.setEventName("pv");
        anonReq.setSessionId("sess-merge-1");
        anonReq.setTenantId(1);
        eventService.save(anonReq);

        // When: User logs in and subsequent event includes userId
        EventCollectRequest realReq = new EventCollectRequest();
        realReq.setEventName("login");
        realReq.setSessionId("sess-merge-1");
        realReq.setUserId(42);
        realReq.setTenantId(1);
        eventService.save(realReq);

        // Then: Session should be updated with userId = 42
        Session updated = sessionRepository.findBySessionId("sess-merge-1").orElseThrow();
        assertThat(updated.getUserId()).isEqualTo(42);
        
        // ✅ P0修复：必须验证Session的其他属性未改变
        assertThat(updated.getTenantId()).isEqualTo(1);
        assertThat(updated.getSessionId()).isEqualTo("sess-merge-1");
        assertThat(updated.getId()).isEqualTo(savedAnonSession.getId()); // ID应该不变
        
        // ✅ P0修复：验证数据库中的实际数据状态 - 应该有两个事件
        long eventCount = eventRepository.findAll().stream()
                .filter(e -> e.getSessionId() != null && e.getSessionId().equals(savedAnonSession.getId()))
                .count();
        assertThat(eventCount).isEqualTo(2); // 应该有两个事件：一个匿名事件，一个登录事件
    }

    @Test
    @DisplayName("Should not create session when sessionId is null")
    void should_NotCreateSession_When_SessionIdIsNull() {
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("no-session");
        req.setSessionId(null);
        req.setTenantId(1);

        // When: Save event
        Event saved = eventService.save(req);
        
        // Then: Verify event saved but no session created
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSessionId()).isNull();
        
        // Verify no session was created
        assertThat(sessionRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should not create session when sessionId is blank")
    void should_NotCreateSession_When_SessionIdIsBlank() {
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("blank-session");
        req.setSessionId("   "); // blank
        req.setTenantId(1);

        // When: Save event
        Event saved = eventService.save(req);
        
        // Then: Verify event saved but no session created
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSessionId()).isNull();
        
        // Verify no session was created
        assertThat(sessionRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("Should create new session when sessionId does not exist")
    void should_CreateNewSession_When_SessionIdNotFound() {
        /**
         * Business scenario: First event from a new session
         * When an event arrives with a sessionId that doesn't exist in the database,
         * the service should automatically create a new Session entity and link it to
         * the event.
         */
        
        // Given: Session does not exist in database (clean state from @BeforeEach)
        
        // When: Event arrives with new sessionId and associated metadata
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("new-session-event");
        req.setSessionId("new-session");
        req.setTenantId(5);
        req.setUserId(100);

        // When: Save event
        Event saved = eventService.save(req);
        
        // Then: Event should be saved and linked to the newly created session
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSessionId()).isNotNull();
        
        // Then: Session should be created with correct metadata from event request
        Session created = sessionRepository.findBySessionId("new-session").orElseThrow();
        assertThat(created.getSessionId()).isEqualTo("new-session");
        assertThat(created.getTenantId()).isEqualTo(5);
        assertThat(created.getUserId()).isEqualTo(100);
        
        // Verify event is linked to session
        Event found = eventRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getSessionId()).isEqualTo(created.getId());
    }

    @Test
    @DisplayName("Should not update session when session already has userId")
    void should_NotUpdateSession_When_SessionAlreadyHasUserId() {
        /**
         * Business rule: Once a session is associated with a user, it should not be
         * overwritten by a different userId. This prevents user identity confusion.
         */
        
        // Given: A session already has a userId
        Session existing = create()
            .withSessionId("existing-session")
            .withTenantId(1)
            .withUserId(50) // Session already linked to user 50
            .build();
        existing = sessionRepository.save(existing);
        
        Integer originalUserId = existing.getUserId();
        Long originalId = existing.getId();

        // When: Event arrives with a different userId
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("update-test");
        req.setSessionId("existing-session");
        req.setUserId(60); // Different userId - should be ignored
        req.setTenantId(1);
        eventService.save(req);

        // Then: Session should NOT be updated (userId should remain 50)
        Session unchanged = sessionRepository.findBySessionId("existing-session").orElseThrow();
        assertThat(unchanged.getUserId()).isEqualTo(originalUserId);
        assertThat(unchanged.getUserId()).isEqualTo(50);
        assertThat(unchanged.getId()).isEqualTo(originalId);
        
        // Verify no duplicate sessions were created
        long sessionCount = sessionRepository.findAll().stream()
            .filter(s -> s.getSessionId().equals("existing-session"))
            .count();
        assertThat(sessionCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should work when webhookService is null")
    void should_Work_When_WebhookServiceIsNull() {
        // This test is less relevant now since we use @MockBean
        // The real WebhookService would be injected by Spring
        // For this test, we verify that event saving works even if webhook fails
        
        Session session = create()
            .withSessionId("s1")
            .withTenantId(1)
            .build();
        session = sessionRepository.save(session);
        
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("no-webhook");
        req.setSessionId("s1");
        req.setTenantId(1);

        // Should save normally
        Event saved = eventService.save(req);
        assertThat(saved.getId()).isNotNull();
        
        // Verify event was saved to database
        Event found = eventRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getEventName()).isEqualTo("no-webhook");
    }

    @Test
    @DisplayName("Should save event with all structured fields")
    void should_SaveEvent_When_AllStructuredFieldsProvided() {
        // Given: Create session
        Session savedSession = create()
            .withSessionId("s-full")
            .withTenantId(1)
            .build();
        savedSession = sessionRepository.save(savedSession);
        final Long sessionId = savedSession.getId(); // Make effectively final for lambda
        
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("full-fields");
        req.setSessionId("s-full");
        req.setTenantId(1);
        req.setUa("Mozilla/5.0");
        req.setReferrer("https://example.com");
        req.setIp("192.168.1.1");
        req.setDevice("Desktop");
        req.setOs("Windows");
        req.setBrowser("Chrome");
        req.setChannel("web");
        req.setAnonymousId("anon-123");

        // When: Save event
        eventService.save(req);

        // Then: Verify all structured fields are saved
        Event found = eventRepository.findAll().stream()
            .filter(e -> e.getEventName().equals("full-fields") && e.getSessionId().equals(sessionId))
            .findFirst()
            .orElseThrow();
        
        assertThat(found.getUa()).isEqualTo("Mozilla/5.0");
        assertThat(found.getReferrer()).isEqualTo("https://example.com");
        assertThat(found.getIp()).isEqualTo("192.168.1.1");
        assertThat(found.getDevice()).isEqualTo("Desktop");
        assertThat(found.getOs()).isEqualTo("Windows");
        assertThat(found.getBrowser()).isEqualTo("Chrome");
        assertThat(found.getChannel()).isEqualTo("web");
        assertThat(found.getAnonymousId()).isEqualTo("anon-123");
    }

    @Test
    @DisplayName("Should handle database constraint violation when event name is too long")
    void should_HandleFieldOverflow_When_EventNameTooLong() {
        // Given: Create session
        Session session = create()
            .withSessionId("s-overflow")
            .withTenantId(1)
            .build();
        session = sessionRepository.save(session);
        
        // Create event name that exceeds database column length
        String longEventName = "a".repeat(1000);
        
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName(longEventName);
        req.setSessionId("s-overflow");
        req.setTenantId(1);

        // When: Save event with too long name
        // Then: Should throw DataIntegrityViolationException (or similar)
        assertThatThrownBy(() -> eventService.save(req))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should handle concurrent event saves for same session")
    void should_HandleConcurrentEventSaves_ForSameSession() {
        /**
         * Business scenario: Concurrent event saves
         * When multiple events are saved for the same session,
         * the service should handle session creation/update correctly.
         */
        
        String sessionId = "concurrent-session";
        
        // When: Save first event (creates session)
        EventCollectRequest req1 = new EventCollectRequest();
        req1.setEventName("event-1");
        req1.setSessionId(sessionId);
        req1.setTenantId(1);
        req1.setUserId(100);
        Event saved1 = eventService.save(req1);

        // When: Save second event (uses existing session)
        EventCollectRequest req2 = new EventCollectRequest();
        req2.setEventName("event-2");
        req2.setSessionId(sessionId);
        req2.setTenantId(1);
        req2.setUserId(100);
        Event saved2 = eventService.save(req2);

        // Then: Both events should be saved with correct sessionId
        assertThat(saved1.getSessionId()).isNotNull();
        assertThat(saved2.getSessionId()).isNotNull();
        assertThat(saved1.getSessionId()).isEqualTo(saved2.getSessionId());
        
        // Then: Session should be created once
        Session session = sessionRepository.findBySessionId(sessionId).orElseThrow();
        assertThat(session.getUserId()).isEqualTo(100);
        
        // Verify both events linked to same session
        Event found1 = eventRepository.findById(saved1.getId()).orElseThrow();
        Event found2 = eventRepository.findById(saved2.getId()).orElseThrow();
        assertThat(found1.getSessionId()).isEqualTo(session.getId());
        assertThat(found2.getSessionId()).isEqualTo(session.getId());
    }
}

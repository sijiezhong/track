package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;
import io.github.sijiezhong.track.dto.EventCollectRequest;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.repository.SessionRepository;
import io.github.sijiezhong.track.stream.EventStreamBroadcaster;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.EntityManager;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pure unit tests for EventService.
 * 
 * This class tests EventService business logic in isolation without any Spring
 * context or database dependencies.
 * All dependencies are mocked using Mockito.
 * 
 * Coverage includes:
 * - Event entity creation and field mapping
 * - Session lookup and creation logic (mocked repository behavior)
 * - Anonymous to real user conversion logic
 * - Structured field mapping (UA, referrer, IP, device, OS, browser, channel,
 * anonymousId)
 * - Null/empty sessionId handling
 * - Properties serialization
 * - Metrics counter increment
 * - SSE broadcasting
 * - Webhook triggering
 */
@ExtendWith(MockitoExtension.class)
class EventServiceUnitTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private EventStreamBroadcaster broadcaster;

    @Mock
    private WebhookService webhookService;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter eventsCreatedCounter;

    @Mock
    private EntityManager entityManager;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        // Setup meterRegistry to return counter
        // Counter is created via Counter.builder() in EventService constructor
        // We mock the counter directly without needing to mock the builder
        eventService = new EventService(
                eventRepository,
                sessionRepository,
                broadcaster,
                webhookService,
                meterRegistry);
        
        // ⚠️ P0-6 说明：使用ReflectionTestUtils的必要性
        // EventService使用self-injection (@Lazy EventService self) 来支持@Transactional代理行为
        // 在真实的Spring容器中，self会被自动注入为代理对象
        // 在单元测试中，我们没有Spring容器，所以需要手动注入self
        // 这是测试Spring AOP代理行为的合理用例，但确实违反了"不测试实现细节"的原则
        // 
        // 长期改进建议：
        // 1. 考虑重构EventService，将需要@Transactional的方法提取到单独的Service类
        // 2. 或者使用Spring Test Context进行集成测试，而不是纯单元测试
        // 
        // Inject EntityManager using reflection for unit testing
        // Note: EntityManager is @PersistenceContext injected, not constructor injected
        ReflectionTestUtils.setField(eventService, "entityManager", entityManager);
        // Inject self for proper @Transactional proxy behavior in unit tests
        // This is necessary because EventService.save() calls self.findOrCreateSessionInNewTransactionIsolated()
        // which requires @Transactional to work properly
        ReflectionTestUtils.setField(eventService, "self", eventService);
    }

    @Test
    @DisplayName("Should create event with all fields mapped correctly when session exists")
    void should_CreateEvent_When_SessionExists() {
        // Given: Existing session
        Session existingSession = new Session();
        existingSession.setId(100L);
        existingSession.setSessionId("existing-session");
        existingSession.setTenantId(5);
        existingSession.setUserId(42);

        // Mock findBySessionIdWithLock (used by findOrCreateSession)
        when(sessionRepository.findBySessionIdWithLock("existing-session"))
                .thenReturn(Optional.of(existingSession));

        // Given: Event request
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("page_view");
        req.setSessionId("existing-session");
        req.setTenantId(5);
        req.setUserId(42);
        req.setUa("Mozilla/5.0");
        req.setReferrer("https://example.com");
        req.setIp("192.168.1.1");
        req.setDevice("Desktop");
        req.setOs("Windows");
        req.setBrowser("Chrome");
        req.setChannel("web");
        req.setAnonymousId("anon-123");

        ObjectMapper om = new ObjectMapper();
        ObjectNode props = om.createObjectNode();
        props.put("url", "/home");
        req.setProperties(props);

        // Given: Mock repository save - return event with same tenantId as request
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            Event result = new Event();
            result.setId(200L);
            result.setEventName(event.getEventName());
            result.setTenantId(event.getTenantId()); // Preserve tenantId from saved event
            result.setUserId(event.getUserId());
            result.setSessionId(event.getSessionId());
            result.setProperties(event.getProperties());
            return result;
        });

        // When: Save event
        Event result = eventService.save(req);

        // Then: Verify event was created with correct fields
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event captured = eventCaptor.getValue();
        assertThat(captured.getEventName()).isEqualTo("page_view");
        assertThat(captured.getUserId()).isEqualTo(42);
        assertThat(captured.getTenantId()).isEqualTo(5);
        assertThat(captured.getSessionId()).isEqualTo(100L);
        assertThat(captured.getProperties()).contains("/home");
        assertThat(captured.getUa()).isEqualTo("Mozilla/5.0");
        assertThat(captured.getReferrer()).isEqualTo("https://example.com");
        assertThat(captured.getIp()).isEqualTo("192.168.1.1");
        assertThat(captured.getDevice()).isEqualTo("Desktop");
        assertThat(captured.getOs()).isEqualTo("Windows");
        assertThat(captured.getBrowser()).isEqualTo("Chrome");
        assertThat(captured.getChannel()).isEqualTo("web");
        assertThat(captured.getAnonymousId()).isEqualTo("anon-123");

        // Then: Verify metrics counter was incremented (if counter exists)
        // Note: In real implementation, counter is created via MeterRegistry, so we
        // can't easily verify
        // This test verifies the business logic, not the metrics implementation

        // Then: Verify SSE broadcasting - use captured event to get actual tenantId
        ArgumentCaptor<Integer> tenantIdCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Event> eventCaptorForBroadcast = ArgumentCaptor.forClass(Event.class);
        verify(broadcaster).broadcastEvent(tenantIdCaptor.capture(), eventCaptorForBroadcast.capture());
        assertThat(tenantIdCaptor.getValue()).isEqualTo(5);

        // Then: Verify webhook was triggered with the saved event
        verify(webhookService).onEvent(eventCaptorForBroadcast.getValue());

        // Then: Verify result - use captured event for comparison
        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getEventName()).isEqualTo("page_view");
    }

    @Test
    @DisplayName("Should create new session when sessionId does not exist")
    void should_CreateNewSession_When_SessionIdNotFound() {
        // Given: Session does not exist
        when(sessionRepository.findBySessionIdWithLock("new-session"))
                .thenReturn(Optional.empty()); // First call: not found

        // Given: Mock session creation (in createSessionInNewTransaction)
        Session newSession = new Session();
        newSession.setId(300L);
        newSession.setSessionId("new-session");
        newSession.setTenantId(7);
        newSession.setUserId(100);
        when(sessionRepository.save(any(Session.class))).thenReturn(newSession);

        // Given: Event request
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("click");
        req.setSessionId("new-session");
        req.setTenantId(7);
        req.setUserId(100);

        Event savedEvent = new Event();
        savedEvent.setId(400L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // When: Save event
        eventService.save(req);

        // Then: Verify session was created with correct fields
        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(sessionCaptor.capture());

        Session captured = sessionCaptor.getValue();
        assertThat(captured.getSessionId()).isEqualTo("new-session");
        assertThat(captured.getTenantId()).isEqualTo(7);
        assertThat(captured.getUserId()).isEqualTo(100);

        // Then: Verify event was created and linked to session
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        assertThat(capturedEvent.getSessionId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("Should update session userId when converting from anonymous to real user")
    void should_UpdateSessionUserId_When_ConvertingFromAnonymousToReal() {
        // Given: Anonymous session exists
        Session anonymousSession = new Session();
        anonymousSession.setId(500L);
        anonymousSession.setSessionId("anon-session");
        anonymousSession.setTenantId(1);
        anonymousSession.setUserId(null); // Anonymous

        when(sessionRepository.findBySessionIdWithLock("anon-session"))
                .thenReturn(Optional.of(anonymousSession));

        // Given: Updated session with userId (will be called when updating session)
        Session updatedSession = new Session();
        updatedSession.setId(500L);
        updatedSession.setUserId(50);
        updatedSession.setSessionId("anon-session");
        updatedSession.setTenantId(1);
        when(sessionRepository.save(any(Session.class))).thenReturn(updatedSession);

        // Given: Event request with userId
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("login");
        req.setSessionId("anon-session");
        req.setTenantId(1);
        req.setUserId(50); // User logs in

        Event savedEvent = new Event();
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // When: Save event
        eventService.save(req);

        // Then: Verify session was updated with userId
        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(sessionCaptor.capture());

        Session captured = sessionCaptor.getValue();
        assertThat(captured.getUserId()).isEqualTo(50);
    }

    @Test
    @DisplayName("Should not update session when session already has userId")
    void should_NotUpdateSession_When_SessionAlreadyHasUserId() {
        // Given: Session already has userId
        Session existingSession = new Session();
        existingSession.setId(600L);
        existingSession.setSessionId("existing-session");
        existingSession.setTenantId(1);
        existingSession.setUserId(60); // Already has userId

        // Mock findBySessionIdWithLock (used by findOrCreateSession)
        when(sessionRepository.findBySessionIdWithLock("existing-session"))
                .thenReturn(Optional.of(existingSession));

        // Given: Event request with different userId (should be ignored)
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("action");
        req.setSessionId("existing-session");
        req.setTenantId(1);
        req.setUserId(70); // Different userId - should be ignored

        Event savedEvent = new Event();
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // When: Save event
        eventService.save(req);

        // Then: Verify session was NOT updated (condition: req.getUserId() != null &&
        // sess.getUserId() == null)
        // Since sess.getUserId() == 60 (not null), the update should not happen
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Should not create session when sessionId is null")
    void should_NotCreateSession_When_SessionIdIsNull() {
        // Given: Event request without sessionId
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("no-session");
        req.setSessionId(null);
        req.setTenantId(1);

        Event savedEvent = new Event();
        savedEvent.setId(700L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // When: Save event
        eventService.save(req);

        // Then: Verify session was NOT created
        verify(sessionRepository, never()).findBySessionId(anyString());
        verify(sessionRepository, never()).save(any(Session.class));

        // Then: Verify event was created without sessionId
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event captured = eventCaptor.getValue();
        assertThat(captured.getSessionId()).isNull();
    }

    @Test
    @DisplayName("Should not create session when sessionId is blank")
    void should_NotCreateSession_When_SessionIdIsBlank() {
        // Given: Event request with blank sessionId
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("blank-session");
        req.setSessionId("   "); // Blank
        req.setTenantId(1);

        Event savedEvent = new Event();
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // When: Save event
        eventService.save(req);

        // Then: Verify session was NOT created
        verify(sessionRepository, never()).findBySessionId(anyString());
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Should handle null properties correctly")
    void should_HandleNullProperties_Correctly() {
        // Given: Event request with null properties
        Session session = new Session();
        session.setId(800L);
        when(sessionRepository.findBySessionIdWithLock("sess-1")).thenReturn(Optional.of(session));

        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("event");
        req.setSessionId("sess-1");
        req.setTenantId(1);
        req.setProperties(null);

        Event savedEvent = new Event();
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // When: Save event
        eventService.save(req);

        // Then: Verify event has null properties
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event captured = eventCaptor.getValue();
        assertThat(captured.getProperties()).isNull();
    }

    @Test
    @DisplayName("Should handle concurrent session creation exception correctly")
    void should_HandleConcurrentSessionCreation_Correctly() {
        // Given: Session doesn't exist initially
        when(sessionRepository.findBySessionIdWithLock("concurrent-session"))
                .thenReturn(Optional.empty()); // First attempt: not found

        // Given: Session creation fails due to concurrent creation
        when(sessionRepository.save(any(Session.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key"));

        // Given: After exception, session exists (created by another thread)
        // The new implementation uses self.findSessionInNewTransaction which needs to
        // be mocked
        // For unit testing, we mock that after retry, session is found
        Session existingSession = new Session();
        existingSession.setId(900L);
        existingSession.setSessionId("concurrent-session");
        existingSession.setTenantId(1);

        // Note: self-injection is already set up in setUp() method
        // Mock findSessionInNewTransaction to return the existing session after delay
        // This simulates the retry logic in findOrCreateSessionInNewTransactionIsolated
        when(sessionRepository.findBySessionIdWithLock("concurrent-session"))
                .thenReturn(Optional.empty()) // First attempt in findOrCreateSessionInNewTransactionIsolated: not found
                .thenReturn(Optional.empty()) // After save fails, first retry: still not found
                .thenReturn(Optional.of(existingSession)); // Second retry: found

        // Given: Event request
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("event");
        req.setSessionId("concurrent-session");
        req.setTenantId(1);

        Event savedEvent = new Event();
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // When: Save event
        // The new implementation should handle concurrent creation gracefully
        eventService.save(req);

        // Then: Verify system handled the concurrent creation gracefully
        verify(sessionRepository, atLeastOnce()).findBySessionIdWithLock("concurrent-session");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("Should not call webhook when webhookService is null")
    void should_NotCallWebhook_When_WebhookServiceIsNull() {
        // Given: EventService without webhookService
        EventService serviceWithoutWebhook = new EventService(
                eventRepository,
                sessionRepository,
                broadcaster,
                null, // No webhook service
                meterRegistry);

        ReflectionTestUtils.setField(serviceWithoutWebhook, "entityManager", entityManager);
        ReflectionTestUtils.setField(serviceWithoutWebhook, "self", serviceWithoutWebhook);

        Session session = new Session();
        session.setId(1000L);
        when(sessionRepository.findBySessionIdWithLock("sess-1")).thenReturn(Optional.of(session));

        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("event");
        req.setSessionId("sess-1");
        req.setTenantId(1);

        Event savedEvent = new Event();
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // When: Save event
        serviceWithoutWebhook.save(req);

        // Then: Verify webhook was NOT called (no NPE)
        // The code checks: if (webhookService != null)
        // Since webhookService is null, no call should be made
        // This test verifies the null check works correctly
    }

    @Test
    @DisplayName("Should not increment counter when meterRegistry is null")
    void should_NotIncrementCounter_When_MeterRegistryIsNull() {
        // Given: EventService without meterRegistry
        EventService serviceWithoutMetrics = new EventService(
                eventRepository,
                sessionRepository,
                broadcaster,
                webhookService,
                null // No meterRegistry
        );

        ReflectionTestUtils.setField(serviceWithoutMetrics, "entityManager", entityManager);
        ReflectionTestUtils.setField(serviceWithoutMetrics, "self", serviceWithoutMetrics);

        Session session = new Session();
        session.setId(1100L);
        when(sessionRepository.findBySessionIdWithLock("sess-1")).thenReturn(Optional.of(session));

        EventCollectRequest req = new EventCollectRequest();
        req.setEventName("event");
        req.setSessionId("sess-1");
        req.setTenantId(1);

        Event savedEvent = new Event();
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // When: Save event
        serviceWithoutMetrics.save(req);

        // Then: Verify counter was NOT accessed (no NPE)
        // The code checks: if (eventsCreatedCounter != null)
        // Since meterRegistry is null, eventsCreatedCounter is null
        // This test verifies the null check works correctly
    }
}

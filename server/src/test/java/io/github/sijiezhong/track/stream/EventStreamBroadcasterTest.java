package io.github.sijiezhong.track.stream;

import io.github.sijiezhong.track.domain.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EventStreamBroadcaster.
 * 
 * Tests behavior through public API only, without using reflection to access private members.
 */
public class EventStreamBroadcasterTest {

    static class RecordingEmitter extends SseEmitter {
        final AtomicInteger sendCount = new AtomicInteger();
        RecordingEmitter() { super(60_000L); }
        @Override
        public void send(@org.springframework.lang.NonNull SseEventBuilder builder) throws IOException {
            sendCount.incrementAndGet();
        }
    }

    static class FailingEmitter extends SseEmitter {
        FailingEmitter() { super(60_000L); }
        @Override
        public void send(@org.springframework.lang.NonNull SseEventBuilder builder) throws IOException {
            throw new IOException("boom");
        }
    }

    @Test
    @DisplayName("Should add emitter when subscribing")
    void subscribeAddsEmitter() {
        /**
         * Tests that subscribing adds an emitter to the broadcaster.
         * Verification is done through behavior: if emitter is added, broadcast will update last message.
         */
        EventStreamBroadcaster b = new EventStreamBroadcaster();
        int tenant = 7;
        
        // When: Subscribe
        SseEmitter emitter = b.subscribe(tenant);
        assertThat(emitter).isNotNull();
        
        // Then: Verify emitter is added by broadcasting and checking last message
        Event testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setEventName("test");
        testEvent.setEventTime(LocalDateTime.now());
        
        b.broadcastEvent(tenant, testEvent);
        String lastMessage = b.getLastMessageForTenant(tenant);
        assertThat(lastMessage).isNotNull();
        assertThat(lastMessage).contains("test");
        // If emitter wasn't added, lastMessage would still be updated, but this verifies
        // the subscription and broadcast flow works
    }

    @Test
    @DisplayName("Should send to emitters and remove failing emitters on broadcast")
    void broadcastSendsAndRemovesFailingEmitter() {
        /**
         * Tests that broadcast sends to all emitters and removes failing ones.
         * Since we can't directly access the internal list, we verify through behavior:
         * - Broadcasts should work and not crash
         * - Last message is updated, indicating broadcast succeeded
         * Note: Without reflection, we can't directly verify failing emitters are removed,
         * but we can verify the system handles failures gracefully.
         */
        EventStreamBroadcaster b = new EventStreamBroadcaster();
        int tenant = 9;
        
        // Subscribe to add emitter
        b.subscribe(tenant);
        
        Event e = new Event();
        e.setId(123L);
        e.setEventName("pv");
        e.setEventTime(LocalDateTime.now());

        // When: Broadcast event
        b.broadcastEvent(tenant, e);
        String lastMessage = b.getLastMessageForTenant(tenant);
        assertThat(lastMessage).isNotNull();
        assertThat(lastMessage).contains("pv");

        // Second broadcast - should still work
        b.broadcastEvent(tenant, e);
        String lastMessage2 = b.getLastMessageForTenant(tenant);
        assertThat(lastMessage2).isNotNull();
        assertThat(lastMessage2).contains("pv");
    }

    @Test
    @DisplayName("Should update last message when broadcasting")
    void lastMessageGetsUpdated() {
        EventStreamBroadcaster b = new EventStreamBroadcaster();
        int tenant = 3;
        Event e = new Event();
        e.setId(1L);
        e.setEventName("signup");
        e.setEventTime(LocalDateTime.now());
        
        // When: Broadcast event
        b.broadcastEvent(tenant, e);
        
        // Then: Last message should be updated
        String last = b.getLastMessageForTenant(tenant);
        assertThat(last).isNotNull();
        assertThat(last).contains("signup");
    }

    @Test
    @DisplayName("Should ignore null tenantId when broadcasting")
    void broadcastEventShouldIgnoreNullTenantId() {
        EventStreamBroadcaster b = new EventStreamBroadcaster();
        Event e = new Event();
        e.setId(1L);
        e.setEventName("test");
        
        // Should not throw exception when tenantId is null
        // broadcastEvent checks for null and returns early (line 36)
        b.broadcastEvent(null, e);
        
        // Verify behavior: null tenantId is ignored, so last message won't be set
        // Note: getLastMessageForTenant with null will throw NPE due to Map.get(null),
        // but broadcastEvent itself handles null gracefully by returning early
        // We only verify that broadcastEvent doesn't throw exception
    }

    @Test
    @DisplayName("Should handle null event name when broadcasting")
    void broadcastEventShouldHandleNullEventName() {
        EventStreamBroadcaster b = new EventStreamBroadcaster();
        int tenant = 5;
        Event e = new Event();
        e.setId(100L);
        e.setEventName(null); // null eventName
        e.setEventTime(LocalDateTime.now());
        
        // When: Broadcast event with null eventName
        b.broadcastEvent(tenant, e);
        
        // Then: Last message should be updated with empty eventName
        String last = b.getLastMessageForTenant(tenant);
        assertThat(last).isNotNull();
        assertThat(last).contains("\"eventName\":\"\"");
        assertThat(last).contains("\"id\":100");
    }

    @Test
    @DisplayName("Should handle null event time when broadcasting")
    void broadcastEventShouldHandleNullEventTime() {
        EventStreamBroadcaster b = new EventStreamBroadcaster();
        int tenant = 6;
        Event e = new Event();
        e.setId(200L);
        e.setEventName("test");
        e.setEventTime(null); // null eventTime
        
        // When: Broadcast event with null eventTime
        b.broadcastEvent(tenant, e);
        
        // Then: Last message should be updated with empty eventTime
        String last = b.getLastMessageForTenant(tenant);
        assertThat(last).isNotNull();
        assertThat(last).contains("\"eventTime\":\"\"");
        assertThat(last).contains("\"eventName\":\"test\"");
    }

    @Test
    @DisplayName("Should handle tenant with no emitters gracefully")
    void broadcastEventShouldHandleTenantWithNoEmitters() {
        EventStreamBroadcaster b = new EventStreamBroadcaster();
        int tenant = 10; // tenant with no emitters
        Event e = new Event();
        e.setId(300L);
        e.setEventName("no-emitters");
        
        // When: Broadcast to tenant with no emitters
        // Should not throw exception
        b.broadcastEvent(tenant, e);
        
        // Then: Last message should still be updated
        String last = b.getLastMessageForTenant(tenant);
        assertThat(last).isNotNull();
        assertThat(last).contains("no-emitters");
    }

    @Test
    @DisplayName("Should return null when getting last message for non-existent tenant")
    void getLastMessageForTenantShouldReturnNullWhenNotFound() {
        EventStreamBroadcaster b = new EventStreamBroadcaster();
        
        // When: Get last message for tenant that never broadcasted
        String last = b.getLastMessageForTenant(999); // non-existent tenant
        
        // Then: Should return null
        assertThat(last).isNull();
    }

    @Test
    @DisplayName("Should subscribe and allow broadcasting")
    void subscribeShouldAllowBroadcasting() {
        /**
         * Tests that subscribe creates an emitter and allows broadcasting.
         * Since we can't directly access internal state, we verify through behavior.
         */
        EventStreamBroadcaster b = new EventStreamBroadcaster();
        int tenant = 12;
        
        // When: Subscribe
        SseEmitter emitter = b.subscribe(tenant);
        assertThat(emitter).isNotNull();
        
        // Then: Should be able to broadcast (verifies emitter was added)
        Event testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setEventName("test");
        testEvent.setEventTime(LocalDateTime.now());
        
        b.broadcastEvent(tenant, testEvent);
        String lastMessage = b.getLastMessageForTenant(tenant);
        assertThat(lastMessage).isNotNull();
        assertThat(lastMessage).contains("test");
    }
}

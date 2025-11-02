package io.github.sijiezhong.track.testsupport;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;

import java.time.LocalDateTime;

import static io.github.sijiezhong.track.testsupport.TestConstants.*;

/**
 * Test fixtures providing pre-configured test data for common scenarios.
 * These fixtures are optimized for common test scenarios.
 */
public final class TestFixtures {
    
    private TestFixtures() {
        // Utility class, prevent instantiation
    }
    
    // Session Fixtures
    
    /**
     * Creates a basic session for tenant 1.
     */
    public static Session createBasicSession() {
        return SessionTestBuilder.create()
            .withSessionId(DEFAULT_SESSION_ID)
            .withTenantId(DEFAULT_TENANT_ID)
            .build();
    }
    
    /**
     * Creates a session for a specific tenant.
     */
    public static Session createSessionForTenant(Integer tenantId) {
        return SessionTestBuilder.sessionForTenant(tenantId);
    }
    
    /**
     * Creates an anonymous session (no user ID).
     */
    public static Session createAnonymousSession() {
        return SessionTestBuilder.anonymousSession();
    }
    
    /**
     * Creates a session with a user ID.
     */
    public static Session createSessionWithUser(Integer userId) {
        return SessionTestBuilder.sessionWithUser(userId);
    }
    
    // Event Fixtures
    
    /**
     * Creates a basic page view event.
     */
    public static Event createBasicEvent() {
        return EventTestBuilder.basicPageViewEvent();
    }
    
    /**
     * Creates an event with custom properties JSON.
     */
    public static Event createEventWithProperties(String propertiesJson) {
        return EventTestBuilder.eventWithProperties(propertiesJson);
    }
    
    /**
     * Creates an event for a specific tenant and event name.
     */
    public static Event createEventForTenant(Integer tenantId, String eventName) {
        return EventTestBuilder.eventForTenant(tenantId, eventName);
    }
    
    /**
     * Creates an event with all structured fields populated.
     */
    public static Event createEventWithAllFields() {
        return EventTestBuilder.eventWithAllFields();
    }
    
    /**
     * Creates a simple event for funnel testing.
     * Used in EventFunnelAnalyticsIntegrationTest scenarios.
     */
    public static Event createFunnelEvent(String eventName, Integer tenantId, Long sessionId, LocalDateTime eventTime) {
        return EventTestBuilder.create()
            .withEventName(eventName)
            .withTenantId(tenantId)
            .withSessionId(sessionId)
            .withEventTime(eventTime)
            .withProperties(DEFAULT_PROPERTIES)
            .build();
    }
    
    /**
     * Creates a click event with standard properties.
     */
    public static Event createClickEvent() {
        return EventTestBuilder.create()
            .withEventName(EVENT_CLICK)
            .withTenantId(DEFAULT_TENANT_ID)
            .withProperties(PROPERTIES_WITH_PRODUCT)
            .build();
    }
    
    /**
     * Creates a purchase event.
     */
    public static Event createPurchaseEvent() {
        return EventTestBuilder.create()
            .withEventName(EVENT_PURCHASE)
            .withTenantId(DEFAULT_TENANT_ID)
            .withUserId(DEFAULT_USER_ID)
            .withProperties("{\"amount\":99.99,\"currency\":\"USD\"}")
            .build();
    }
    
    /**
     * Creates an event with full structured data (UA, referrer, IP, device, OS, browser).
     */
    public static Event createEventWithStructuredData() {
        return EventTestBuilder.create()
            .withEventName(EVENT_PAGE_VIEW)
            .withTenantId(DEFAULT_TENANT_ID)
            .withUa(UA_CHROME)
            .withReferrer(DEFAULT_REFERRER)
            .withIp(DEFAULT_IP)
            .withDevice(DEVICE_DESKTOP)
            .withOs(OS_WINDOWS)
            .withBrowser(BROWSER_CHROME)
            .withChannel(CHANNEL_WEB)
            .withProperties(PROPERTIES_WITH_URL)
            .build();
    }
    
    /**
     * Creates a mobile event with mobile-specific data.
     */
    public static Event createMobileEvent() {
        return EventTestBuilder.create()
            .withEventName(EVENT_PAGE_VIEW)
            .withTenantId(DEFAULT_TENANT_ID)
            .withUa(UA_MOBILE)
            .withDevice(DEVICE_MOBILE)
            .withOs(OS_ANDROID)
            .withChannel(CHANNEL_MOBILE)
            .withProperties(PROPERTIES_WITH_URL)
            .build();
    }
}


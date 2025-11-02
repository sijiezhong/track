package io.github.sijiezhong.track.testsupport;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;

import java.time.LocalDateTime;

import static io.github.sijiezhong.track.testsupport.TestConstants.*;

/**
 * Builder for creating Event test objects with a fluent interface.
 * 
 * Usage example:
 * <pre>
 * Event event = EventTestBuilder.create()
 *     .withEventName("page_view")
 *     .withTenantId(1)
 *     .withSession(session)
 *     .withProperties("{\"url\":\"/home\"}")
 *     .build();
 * </pre>
 */
public class EventTestBuilder {
    
    private final Event event;
    
    private EventTestBuilder() {
        this.event = new Event();
        // Set sensible defaults
        this.event.setEventName(EVENT_PAGE_VIEW);
        this.event.setTenantId(DEFAULT_TENANT_ID);
        this.event.setProperties(DEFAULT_PROPERTIES);
        this.event.setEventTime(FIXED_TIME); // Use fixed time for test reproducibility
    }
    
    /**
     * Creates a new EventTestBuilder instance.
     */
    public static EventTestBuilder create() {
        return new EventTestBuilder();
    }
    
    /**
     * Sets the event name.
     */
    public EventTestBuilder withEventName(String eventName) {
        this.event.setEventName(eventName);
        return this;
    }
    
    /**
     * Sets the tenant ID.
     */
    public EventTestBuilder withTenantId(Integer tenantId) {
        this.event.setTenantId(tenantId);
        return this;
    }
    
    /**
     * Sets the user ID.
     */
    public EventTestBuilder withUserId(Integer userId) {
        this.event.setUserId(userId);
        return this;
    }
    
    /**
     * Sets the session ID (by Long ID).
     */
    public EventTestBuilder withSessionId(Long sessionId) {
        this.event.setSessionId(sessionId);
        return this;
    }
    
    /**
     * Sets the session ID (by Session object, extracts the ID).
     */
    public EventTestBuilder withSession(Session session) {
        if (session != null && session.getId() != null) {
            this.event.setSessionId(session.getId());
        }
        return this;
    }
    
    /**
     * Sets the properties JSON string.
     */
    public EventTestBuilder withProperties(String properties) {
        this.event.setProperties(properties);
        return this;
    }
    
    /**
     * Sets the user agent.
     */
    public EventTestBuilder withUa(String ua) {
        this.event.setUa(ua);
        return this;
    }
    
    /**
     * Sets the referrer.
     */
    public EventTestBuilder withReferrer(String referrer) {
        this.event.setReferrer(referrer);
        return this;
    }
    
    /**
     * Sets the IP address.
     */
    public EventTestBuilder withIp(String ip) {
        this.event.setIp(ip);
        return this;
    }
    
    /**
     * Sets the device type.
     */
    public EventTestBuilder withDevice(String device) {
        this.event.setDevice(device);
        return this;
    }
    
    /**
     * Sets the operating system.
     */
    public EventTestBuilder withOs(String os) {
        this.event.setOs(os);
        return this;
    }
    
    /**
     * Sets the browser.
     */
    public EventTestBuilder withBrowser(String browser) {
        this.event.setBrowser(browser);
        return this;
    }
    
    /**
     * Sets the channel.
     */
    public EventTestBuilder withChannel(String channel) {
        this.event.setChannel(channel);
        return this;
    }
    
    /**
     * Sets the anonymous ID.
     */
    public EventTestBuilder withAnonymousId(String anonymousId) {
        this.event.setAnonymousId(anonymousId);
        return this;
    }
    
    /**
     * Sets the event time.
     */
    public EventTestBuilder withEventTime(LocalDateTime eventTime) {
        this.event.setEventTime(eventTime);
        return this;
    }
    
    /**
     * Sets the create time.
     */
    public EventTestBuilder withCreateTime(LocalDateTime createTime) {
        this.event.setCreateTime(createTime);
        return this;
    }
    
    /**
     * Sets the update time.
     */
    public EventTestBuilder withUpdateTime(LocalDateTime updateTime) {
        this.event.setUpdateTime(updateTime);
        return this;
    }
    
    /**
     * Builds and returns the Event object.
     */
    public Event build() {
        return event;
    }
    
    // Convenience methods for common scenarios
    
    /**
     * Creates a basic page view event for the default tenant.
     */
    public static Event basicPageViewEvent() {
        return create()
            .withEventName(EVENT_PAGE_VIEW)
            .withTenantId(DEFAULT_TENANT_ID)
            .withProperties(PROPERTIES_WITH_URL)
            .build();
    }
    
    /**
     * Creates an event with properties JSON.
     */
    public static Event eventWithProperties(String propertiesJson) {
        return create()
            .withEventName(EVENT_PAGE_VIEW)
            .withTenantId(DEFAULT_TENANT_ID)
            .withProperties(propertiesJson)
            .build();
    }
    
    /**
     * Creates an event for a specific tenant.
     */
    public static Event eventForTenant(Integer tenantId, String eventName) {
        return create()
            .withEventName(eventName)
            .withTenantId(tenantId)
            .withProperties(DEFAULT_PROPERTIES)
            .build();
    }
    
    /**
     * Creates an event with all structured fields populated.
     */
    public static Event eventWithAllFields() {
        return create()
            .withEventName(EVENT_PAGE_VIEW)
            .withTenantId(DEFAULT_TENANT_ID)
            .withUserId(DEFAULT_USER_ID)
            .withProperties(PROPERTIES_WITH_URL)
            .withUa(DEFAULT_UA)
            .withReferrer(DEFAULT_REFERRER)
            .withIp(DEFAULT_IP)
            .withDevice(DEVICE_DESKTOP)
            .withOs(OS_MAC)
            .withBrowser(BROWSER_CHROME)
            .withChannel(CHANNEL_WEB)
            .build();
    }
}


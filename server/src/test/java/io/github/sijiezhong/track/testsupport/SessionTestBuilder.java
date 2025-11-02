package io.github.sijiezhong.track.testsupport;

import io.github.sijiezhong.track.domain.Session;

import java.time.LocalDateTime;

import static io.github.sijiezhong.track.testsupport.TestConstants.*;

/**
 * Builder for creating Session test objects with a fluent interface.
 * 
 * Usage example:
 * <pre>
 * Session session = SessionTestBuilder.create()
 *     .withSessionId("my-session")
 *     .withTenantId(1)
 *     .withUserId(10)
 *     .build();
 * </pre>
 */
public class SessionTestBuilder {
    
    private final Session session;
    
    private SessionTestBuilder() {
        this.session = new Session();
        // Set sensible defaults
        this.session.setTenantId(DEFAULT_TENANT_ID);
        this.session.setSessionId(DEFAULT_SESSION_ID);
    }
    
    /**
     * Creates a new SessionTestBuilder instance.
     */
    public static SessionTestBuilder create() {
        return new SessionTestBuilder();
    }
    
    /**
     * Sets the session ID.
     */
    public SessionTestBuilder withSessionId(String sessionId) {
        this.session.setSessionId(sessionId);
        return this;
    }
    
    /**
     * Sets the tenant ID.
     */
    public SessionTestBuilder withTenantId(Integer tenantId) {
        this.session.setTenantId(tenantId);
        return this;
    }
    
    /**
     * Sets the user ID.
     */
    public SessionTestBuilder withUserId(Integer userId) {
        this.session.setUserId(userId);
        return this;
    }
    
    /**
     * Sets the user agent.
     */
    public SessionTestBuilder withUserAgent(String userAgent) {
        this.session.setUserAgent(userAgent);
        return this;
    }
    
    /**
     * Sets the IP address.
     */
    public SessionTestBuilder withIp(String ip) {
        this.session.setIp(ip);
        return this;
    }
    
    /**
     * Sets the start time.
     */
    public SessionTestBuilder withStartTime(LocalDateTime startTime) {
        this.session.setStartTime(startTime);
        return this;
    }
    
    /**
     * Sets the end time.
     */
    public SessionTestBuilder withEndTime(LocalDateTime endTime) {
        this.session.setEndTime(endTime);
        return this;
    }
    
    /**
     * Sets the create time.
     */
    public SessionTestBuilder withCreateTime(LocalDateTime createTime) {
        this.session.setCreateTime(createTime);
        return this;
    }
    
    /**
     * Sets the update time.
     */
    public SessionTestBuilder withUpdateTime(LocalDateTime updateTime) {
        this.session.setUpdateTime(updateTime);
        return this;
    }
    
    /**
     * Builds and returns the Session object.
     */
    public Session build() {
        return session;
    }
    
    // Convenience methods for common scenarios
    
    /**
     * Creates a session for the default tenant (tenant ID = 1).
     */
    public static Session defaultSession() {
        return create()
            .withSessionId(DEFAULT_SESSION_ID)
            .withTenantId(DEFAULT_TENANT_ID)
            .build();
    }
    
    /**
     * Creates a session for a specific tenant.
     */
    public static Session sessionForTenant(Integer tenantId) {
        return create()
            .withTenantId(tenantId)
            .withSessionId("sess-tenant-" + tenantId)
            .build();
    }
    
    /**
     * Creates an anonymous session (no user ID).
     */
    public static Session anonymousSession() {
        return create()
            .withSessionId("sess-anonymous")
            .withTenantId(DEFAULT_TENANT_ID)
            .withUserId(null)
            .build();
    }
    
    /**
     * Creates a session with a user ID (real user, not anonymous).
     */
    public static Session sessionWithUser(Integer userId) {
        return create()
            .withSessionId("sess-user-" + userId)
            .withTenantId(DEFAULT_TENANT_ID)
            .withUserId(userId)
            .build();
    }
}


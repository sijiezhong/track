package io.github.sijiezhong.track.dto;

import java.time.LocalDateTime;

/**
 * 事件列表简要 DTO
 */
public class EventListItem {
    private Long id;
    private String eventName;
    private Integer userId;
    private Long sessionId;
    private Integer appId;
    private LocalDateTime eventTime;
    private String properties;

    public EventListItem() { }
    public EventListItem(Long id, String eventName, Integer userId, Long sessionId, Integer appId, LocalDateTime eventTime, String properties) {
        this.id = id;
        this.eventName = eventName;
        this.userId = userId;
        this.sessionId = sessionId;
        this.appId = appId;
        this.eventTime = eventTime;
        this.properties = properties;
    }
    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Integer getTenantId() { return appId; }
    public void setTenantId(Integer appId) { this.appId = appId; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    public String getProperties() { return properties; }
    public void setProperties(String properties) { this.properties = properties; }
}

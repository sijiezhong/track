package io.github.sijiezhong.track.dto;

import java.time.LocalDateTime;

public class IdempotentSummary {
    private Long eventId;
    private String eventName;
    private LocalDateTime eventTime;
    public IdempotentSummary() {}
    public IdempotentSummary(Long eventId, String eventName, LocalDateTime eventTime) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventTime = eventTime;
    }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
}

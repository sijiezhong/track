package com.track.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 事件实体
 */
@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "app_id", nullable = false, length = 64)
    private String appId;
    
    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;
    
    @Column(name = "user_properties")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> userProperties;
    
    /**
     * 事件类型ID（对应 EventType 枚举的 code 值）
     * 注意：必须与客户端 EventType 枚举值保持一致
     * 1: PAGE_VIEW, 2: CLICK, 3: PERFORMANCE, 4: ERROR, 5: CUSTOM, 6: PAGE_STAY
     */
    @Column(name = "event_type_id", nullable = false)
    private Short eventTypeId;
    
    /**
     * 获取事件类型枚举
     */
    public EventType getEventType() {
        return EventType.fromCode(this.eventTypeId);
    }
    
    /**
     * 设置事件类型枚举
     */
    public void setEventType(EventType eventType) {
        this.eventTypeId = (short) eventType.getCode();
    }
    
    @Column(name = "custom_event_id", length = 128)
    private String customEventId;
    
    @Column(name = "properties")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> properties;
    
    @Column(name = "dom_path", columnDefinition = "TEXT")
    private String domPath;
    
    @Column(name = "page_url", columnDefinition = "TEXT")
    private String pageUrl;
    
    @Column(name = "page_title", columnDefinition = "TEXT")
    private String pageTitle;
    
    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    @Column(name = "ip_address")
    private InetAddress ipAddress;
    
    @Column(name = "server_timestamp", nullable = false)
    private LocalDateTime serverTimestamp;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (serverTimestamp == null) {
            serverTimestamp = LocalDateTime.now();
        }
    }
}


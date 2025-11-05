package com.track.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 批量事件请求 DTO（客户端发送，不再包含用户信息）
 */
@Data
public class TrackBatchRequest {
    // 注意：不再包含 a(appId)、u(userId)、up(userProps)
    // 这些信息从 Cookie 中的 sessionId 获取
    private List<EventDTO> e; // events
    
    // 服务端补充字段（从 Session 和请求中获取）
    private String appId;      // 从 Session 获取
    private String userId;     // 从 Session 获取
    private Map<String, Object> userProps; // 从 Session 获取
    private LocalDateTime serverTimestamp;
    private String ipAddress;
    private String userAgent;
    
    @Data
    public static class EventDTO {
        /**
         * 事件类型（对应 EventType 枚举的 code 值）
         * 1: PAGE_VIEW, 2: CLICK, 3: PERFORMANCE, 4: ERROR, 5: CUSTOM, 6: PAGE_STAY
         * 注意：必须与客户端 EventType 枚举值保持一致
         */
        private Integer t; // type
        private String id; // custom event id
        private Map<String, Object> p; // properties
    }
}


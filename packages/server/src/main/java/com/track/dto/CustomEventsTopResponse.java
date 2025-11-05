package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 自定义事件 TopN 响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomEventsTopResponse {
    private List<CustomEventStats> list;
    private long total;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomEventStats {
        private String eventId;
        private long count;
    }
}


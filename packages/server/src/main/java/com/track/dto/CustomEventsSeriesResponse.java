package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 自定义事件趋势响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomEventsSeriesResponse {
    private List<CustomEventPoint> series;
    private long total;
    private String groupBy;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomEventPoint {
        private String ts;
        private long count;
    }
}


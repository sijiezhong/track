package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Web Vitals 趋势响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebVitalsSeriesResponse {
    private List<WebVitalsPoint> series;
    private String interval;
    private String timezone;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebVitalsPoint {
        private String ts;
        private double p50;
        private double p75;
        private double p95;
    }
}


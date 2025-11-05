package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PV/UV 趋势响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PvUvSeriesResponse {
    private List<TimeSeriesPoint> series;
    private String interval;
    private String timezone;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesPoint {
        private String ts;
        private long pv;
        private long uv;
    }
}


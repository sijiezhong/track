package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 错误趋势响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorsTrendResponse {
    private List<ErrorPoint> series;
    private String interval;
    private String timezone;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorPoint {
        private String ts;
        private long count;
    }
}


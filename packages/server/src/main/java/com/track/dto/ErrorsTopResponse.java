package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 错误 TopN 响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorsTopResponse {
    private List<ErrorStats> list;
    private long total;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorStats {
        private String fingerprint;
        private String message;
        private long count;
        private String firstSeen;
        private String lastSeen;
    }
}


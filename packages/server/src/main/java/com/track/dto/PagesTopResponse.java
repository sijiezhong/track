package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 页面 TopN 响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagesTopResponse {
    private List<PageStats> list;
    private long total;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageStats {
        private String pageUrl;
        private long pv;
        private long uv;
        private double avgDurationSec;
    }
}


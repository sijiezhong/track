package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 事件列表响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventsListResponse {
    private List<Map<String, Object>> items;
    private PageInfo page;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageInfo {
        private int index;
        private int size;
        private long total;
    }
}


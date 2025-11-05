package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 事件类型分布响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventsDistributionResponse {
    private List<TypeDistribution> list;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeDistribution {
        private String type;
        private long value;
    }
}


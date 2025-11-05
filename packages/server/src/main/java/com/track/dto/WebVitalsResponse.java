package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Web Vitals 分位响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebVitalsResponse {
    private double p50;
    private double p75;
    private double p95;
    private String unit;
}


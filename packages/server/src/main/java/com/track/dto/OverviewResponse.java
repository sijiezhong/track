package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 概览响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverviewResponse {
    private long pv;
    private long uv;
    private double bounceRate;
    private double avgDurationSec;
    private String timezone;
}


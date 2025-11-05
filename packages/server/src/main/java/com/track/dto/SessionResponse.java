package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Session 响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private String sessionId;
}


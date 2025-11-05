package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Session 信息（存储在 Redis）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String appId;
    private String userId;
    private Map<String, Object> userProps;
    private int ttlMinutes; // 原始 TTL，用于刷新时保持一致性
}


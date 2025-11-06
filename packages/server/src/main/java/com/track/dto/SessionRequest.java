package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Session 注册请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionRequest {
    private String appId;
    private String appName; // 可选，未提供时使用 appId 作为项目名
    private String userId;
    private Map<String, Object> userProps;
    private Integer ttlMinutes; // 可选，默认 1440（24小时），0 表示不过期
}


package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 应用列表响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectsResponse {
    private List<ProjectInfo> list;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectInfo {
        private String appId;
        private String appName;
    }
}


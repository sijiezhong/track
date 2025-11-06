package com.track.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 通用错误响应 DTO
 */
@Data
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
}



package com.track.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * 全局异常处理器
 * 统一处理异常并返回标准错误响应
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        String traceId = UUID.randomUUID().toString();
        log.error("Illegal argument error [traceId: {}]: {}", traceId, e.getMessage(), e);
        
        ErrorResponse error = new ErrorResponse("BAD_REQUEST", e.getMessage(), traceId);
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        String traceId = UUID.randomUUID().toString();
        log.error("Internal server error [traceId: {}]: {}", traceId, e.getMessage(), e);
        
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "Internal server error", traceId);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    /**
     * 错误响应 DTO
     */
    public static class ErrorResponse {
        private String code;
        private String message;
        private String traceId;
        
        public ErrorResponse(String code, String message, String traceId) {
            this.code = code;
            this.message = message;
            this.traceId = traceId;
        }
        
        public String getCode() {
            return code;
        }
        
        public void setCode(String code) {
            this.code = code;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getTraceId() {
            return traceId;
        }
        
        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }
    }
}


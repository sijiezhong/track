package io.github.sijiezhong.track.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一API错误响应类
 * 
 * <p>所有错误的API响应都应使用此类，确保错误响应格式的统一性。
 * 
 * <p>响应格式示例：
 * <pre>{@code
 * {
 *   "code": 400,
 *   "message": "参数验证失败",
 *   "errorCode": "VALIDATION_ERROR",
 *   "details": ["字段userName不能为空", "字段email格式不正确"],
 *   "timestamp": "2024-01-01T12:00:00"
 * }
 * }</pre>
 * 
 * @author sijie
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    
    private int code;
    private String message;
    private String errorCode;
    private List<String> details;
    private LocalDateTime timestamp;
    
    /**
     * 默认构造函数
     */
    public ApiError() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 构造函数
     * 
     * @param code HTTP状态码
     * @param message 错误消息
     * @param errorCode 错误码标识符
     */
    public ApiError(int code, String message, String errorCode) {
        this.code = code;
        this.message = message;
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 构造函数（带详情）
     * 
     * @param code HTTP状态码
     * @param message 错误消息
     * @param errorCode 错误码标识符
     * @param details 错误详情列表
     */
    public ApiError(int code, String message, String errorCode, List<String> details) {
        this.code = code;
        this.message = message;
        this.errorCode = errorCode;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
    
    public int getCode() {
        return code;
    }
    
    public void setCode(int code) {
        this.code = code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public List<String> getDetails() {
        return details;
    }
    
    public void setDetails(List<String> details) {
        this.details = details;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}


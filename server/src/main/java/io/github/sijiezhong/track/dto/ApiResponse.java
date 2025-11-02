package io.github.sijiezhong.track.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

/**
 * 统一API成功响应包装类
 * 
 * <p>所有成功的API响应都应使用此类进行包装，确保响应格式的统一性。
 * 
 * <p>响应格式示例：
 * <pre>{@code
 * {
 *   "code": 200,
 *   "message": "成功",
 *   "data": {...},
 *   "timestamp": "2024-01-01T12:00:00"
 * }
 * }</pre>
 * 
 * @param <T> 响应数据类型
 * @author sijie
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private int code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    
    /**
     * 默认构造函数
     */
    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 构造函数
     * 
     * @param code 响应码（通常为200表示成功）
     * @param message 响应消息
     * @param data 响应数据
     */
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 创建成功响应（无数据）
     * 
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "成功", null);
    }
    
    /**
     * 创建成功响应（带数据）
     * 
     * @param <T> 数据类型
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "成功", data);
    }
    
    /**
     * 创建成功响应（带自定义消息）
     * 
     * @param <T> 数据类型
     * @param message 自定义消息
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
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
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}


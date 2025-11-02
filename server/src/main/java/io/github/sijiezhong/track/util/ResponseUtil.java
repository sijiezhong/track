package io.github.sijiezhong.track.util;

import io.github.sijiezhong.track.dto.ApiResponse;

/**
 * 响应构建工具类
 * 
 * <p>提供便捷的方法用于构建统一的API响应，简化Controller代码。
 * 
 * @author sijie
 */
public final class ResponseUtil {
    
    private ResponseUtil() {
        // 工具类，禁止实例化
    }
    
    /**
     * 创建成功响应（无数据）
     * 
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success() {
        return ApiResponse.success();
    }
    
    /**
     * 创建成功响应（带数据）
     * 
     * @param <T> 数据类型
     * @param data 响应数据
     * @return 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.success(data);
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
        return ApiResponse.success(message, data);
    }
}


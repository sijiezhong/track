package io.github.sijiezhong.track.exception;

import java.util.List;

/**
 * 参数验证异常
 * 
 * <p>用于表示参数验证失败的情况，可以包含多个验证错误信息。
 * 
 * @author sijie
 */
public class ValidationException extends BaseException {
    
    private final List<String> details;
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     */
    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
        this.details = null;
    }
    
    /**
     * 构造函数（带验证详情）
     * 
     * @param errorCode 错误码
     * @param details 验证错误详情列表
     */
    public ValidationException(ErrorCode errorCode, List<String> details) {
        super(errorCode);
        this.details = details;
    }
    
    /**
     * 构造函数（带自定义消息）
     * 
     * @param errorCode 错误码
     * @param message 自定义错误消息
     */
    public ValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
        this.details = null;
    }
    
    /**
     * 构造函数（带自定义消息和验证详情）
     * 
     * @param errorCode 错误码
     * @param message 自定义错误消息
     * @param details 验证错误详情列表
     */
    public ValidationException(ErrorCode errorCode, String message, List<String> details) {
        super(errorCode, message);
        this.details = details;
    }
    
    /**
     * 获取验证错误详情
     * 
     * @return 验证错误详情列表，可能为null
     */
    public List<String> getDetails() {
        return details;
    }
}


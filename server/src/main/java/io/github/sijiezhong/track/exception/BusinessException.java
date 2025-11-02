package io.github.sijiezhong.track.exception;

/**
 * 业务异常
 * 
 * <p>用于表示业务逻辑错误，如参数验证失败、业务规则违反等。
 * 此类异常会被全局异常处理器捕获并转换为统一的错误响应。
 * 
 * @author sijie
 */
public class BusinessException extends BaseException {
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * 构造函数（带自定义消息）
     * 
     * @param errorCode 错误码
     * @param message 自定义错误消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param errorCode 错误码
     * @param cause 异常原因
     */
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
    
    /**
     * 构造函数（带自定义消息和原因）
     * 
     * @param errorCode 错误码
     * @param message 自定义错误消息
     * @param cause 异常原因
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}


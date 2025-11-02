package io.github.sijiezhong.track.exception;

/**
 * 资源未找到异常
 * 
 * <p>用于表示请求的资源不存在的情况。
 * 
 * @author sijie
 */
public class ResourceNotFoundException extends BaseException {
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码（通常为NOT_FOUND）
     */
    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    /**
     * 构造函数（带自定义消息）
     * 
     * @param errorCode 错误码
     * @param message 自定义错误消息，如 "用户不存在" 或 "事件ID 123 不存在"
     */
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}


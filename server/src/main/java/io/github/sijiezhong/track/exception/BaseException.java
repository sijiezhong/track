package io.github.sijiezhong.track.exception;

/**
 * 基础业务异常类
 * 
 * <p>所有业务异常的基类，提供统一的异常处理机制。
 * 子类应继承此类以实现特定类型的业务异常。
 * 
 * @author sijie
 */
public class BaseException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误码
     */
    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    /**
     * 构造函数（带自定义消息）
     * 
     * @param errorCode 错误码
     * @param message 自定义错误消息
     */
    public BaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param errorCode 错误码
     * @param cause 异常原因
     */
    public BaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造函数（带自定义消息和原因）
     * 
     * @param errorCode 错误码
     * @param message 自定义错误消息
     * @param cause 异常原因
     */
    public BaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 获取错误码
     * 
     * @return 错误码
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}


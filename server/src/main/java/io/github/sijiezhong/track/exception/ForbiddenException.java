package io.github.sijiezhong.track.exception;

/**
 * 禁止访问异常
 * 
 * <p>用于表示用户已认证但没有权限访问资源的情况。
 * 
 * @author sijie
 */
public class ForbiddenException extends BaseException {
    
    /**
     * 构造函数
     */
    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }
    
    /**
     * 构造函数（带自定义消息）
     * 
     * @param message 自定义错误消息，如 "租户ID不匹配"
     */
    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
    
    /**
     * 构造函数（使用特定错误码）
     * 
     * @param errorCode 错误码（如APP_ID_MISMATCH）
     */
    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode);
    }
}


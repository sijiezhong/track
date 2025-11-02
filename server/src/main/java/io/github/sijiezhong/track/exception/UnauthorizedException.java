package io.github.sijiezhong.track.exception;

/**
 * 未授权异常
 * 
 * <p>用于表示用户未提供有效认证信息或认证信息已过期的情况。
 * 
 * @author sijie
 */
public class UnauthorizedException extends BaseException {
    
    /**
     * 构造函数
     */
    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }
    
    /**
     * 构造函数（带自定义消息）
     * 
     * @param message 自定义错误消息
     */
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}


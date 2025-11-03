package io.github.sijiezhong.track.exception;

/**
 * 错误码枚举
 * 
 * <p>定义系统中所有错误码，包括HTTP状态码和业务错误码。
 * 所有错误响应都应使用此枚举中的错误码，确保错误码的一致性和可维护性。
 * 
 * @author sijie
 */
public enum ErrorCode {
    
    // 成功
    SUCCESS(200, "成功"),
    
    // 客户端错误 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源未找到"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),
    CONFLICT(409, "资源冲突"),
    VALIDATION_ERROR(400, "参数验证失败"),
    
    // 业务错误码 (4xx范围，使用400-499)
    APP_ID_REQUIRED(400, "应用ID不能为空"),
    APP_ID_MISMATCH(403, "应用ID不匹配"),
    SESSION_ID_REQUIRED(400, "会话ID不能为空"),
    INVALID_SESSION_ID(400, "无效的会话ID"),
    EVENT_NAME_REQUIRED(400, "事件名称不能为空"),
    INVALID_EVENT_NAME(400, "无效的事件名称"),
    INVALID_PAGE_PARAMS(400, "无效的分页参数"),
    FUNNEL_STEPS_INVALID(400, "漏斗步骤数量必须大于等于2"),
    SEGMENTATION_BY_INVALID(400, "不支持的分组维度"),
    
    // 服务器错误 5xx
    INTERNAL_ERROR(500, "内部服务器错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用");
    
    private final int httpStatus;
    private final String message;
    
    /**
     * 构造函数
     * 
     * @param httpStatus HTTP状态码
     * @param message 错误消息（中文）
     */
    ErrorCode(int httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
    
    /**
     * 获取HTTP状态码
     * 
     * @return HTTP状态码
     */
    public int getHttpStatus() {
        return httpStatus;
    }
    
    /**
     * 获取错误消息
     * 
     * @return 错误消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取错误码名称（作为错误码标识符）
     * 
     * @return 错误码名称，如 "BAD_REQUEST"
     */
    public String getCode() {
        return name();
    }
}


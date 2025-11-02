package io.github.sijiezhong.track.constants;

/**
 * HTTP请求头常量
 * 
 * <p>定义所有HTTP请求头的名称，避免在代码中使用魔法字符串。
 * 
 * @author sijie
 */
public final class HttpHeaderConstants {
    
    private HttpHeaderConstants() {
        // 工具类，禁止实例化
    }
    
    /**
     * 租户ID请求头
     */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    
    /**
     * 幂等键请求头
     */
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    
    /**
     * 认证令牌请求头
     */
    public static final String HEADER_AUTHORIZATION = "Authorization";
    
    /**
     * User-Agent请求头
     */
    public static final String HEADER_USER_AGENT = "User-Agent";
    
    /**
     * Referer请求头
     */
    public static final String HEADER_REFERER = "Referer";
    
    /**
     * X-Forwarded-For请求头（用于获取客户端真实IP）
     */
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
}


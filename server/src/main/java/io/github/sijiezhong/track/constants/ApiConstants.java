package io.github.sijiezhong.track.constants;

/**
 * API相关常量
 * 
 * <p>定义API版本、路径前缀、默认参数等常量，避免魔法字符串和数字。
 * 
 * @author sijie
 */
public final class ApiConstants {
    
    private ApiConstants() {
        // 工具类，禁止实例化
    }
    
    /**
     * API版本
     */
    public static final String API_VERSION = "v1";
    
    /**
     * API路径前缀
     */
    public static final String API_PREFIX = "/api/" + API_VERSION;
    
    /**
     * 默认分页页码（从0开始）
     */
    public static final int DEFAULT_PAGE = 0;
    
    /**
     * 默认分页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 20;
    
    /**
     * 最大分页大小
     */
    public static final int MAX_PAGE_SIZE = 1000;
    
    /**
     * 最小分页大小
     */
    public static final int MIN_PAGE_SIZE = 1;
}


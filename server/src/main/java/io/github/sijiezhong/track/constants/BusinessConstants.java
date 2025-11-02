package io.github.sijiezhong.track.constants;

import java.time.Duration;

/**
 * 业务常量
 * 
 * <p>定义业务相关的常量，如重试次数、超时时间、批量操作限制等。
 * 
 * @author sijie
 */
public final class BusinessConstants {
    
    private BusinessConstants() {
        // 工具类，禁止实例化
    }
    
    /**
     * 默认重试次数
     */
    public static final int DEFAULT_MAX_RETRIES = 5;
    
    /**
     * 会话超时时间（秒）
     */
    public static final long SESSION_TIMEOUT_SECONDS = 1800; // 30分钟
    
    /**
     * 会话超时时间（Duration对象）
     */
    public static final Duration SESSION_TIMEOUT = Duration.ofSeconds(SESSION_TIMEOUT_SECONDS);
    
    /**
     * 批量操作最大数量限制
     */
    public static final int MAX_BATCH_SIZE = 1000;
    
    /**
     * 批量操作默认大小
     */
    public static final int DEFAULT_BATCH_SIZE = 10;
    
    /**
     * 漏斗分析最小步骤数
     */
    public static final int MIN_FUNNEL_STEPS = 2;
    
    /**
     * 事件名称最大长度
     */
    public static final int MAX_EVENT_NAME_LENGTH = 64;
    
    /**
     * 会话ID最大长度
     */
    public static final int MAX_SESSION_ID_LENGTH = 128;
}


package io.github.sijiezhong.track.constants;

/**
 * 事件类型枚举（用于压缩URL参数）
 * 
 * <p>
 * 定义事件类型的完整名称和压缩码的映射关系，用于减少像素上报URL的长度。
 * 前后端共享此枚举，确保压缩码的一致性。
 * 
 * @author sijie
 */
public enum EventTypeEnum {
    /** 页面访问事件 */
    PAGEVIEW("pv"),
    /** 点击事件 */
    CLICK("ck"),
    /** 性能数据事件 */
    PERFORMANCE("pf"),
    /** 错误事件 */
    ERROR("er"),
    /** 自定义事件 */
    CUSTOM("ct");
    
    private final String code;
    
    EventTypeEnum(String code) {
        this.code = code;
    }
    
    /**
     * 获取压缩码
     * 
     * @return 压缩码（2字符）
     */
    public String getCode() {
        return code;
    }
    
    /**
     * 根据完整事件名获取枚举
     * 
     * @param eventName 完整事件名（如 "pageview", "click"）
     * @return 对应的事件类型枚举，未匹配时返回 CUSTOM
     */
    public static EventTypeEnum fromEventName(String eventName) {
        if (eventName == null || eventName.isBlank()) {
            return CUSTOM;
        }
        String lower = eventName.toLowerCase().trim();
        switch (lower) {
            case "pageview": return PAGEVIEW;
            case "click": return CLICK;
            case "performance": return PERFORMANCE;
            case "error": return ERROR;
            default: return CUSTOM;
        }
    }
    
    /**
     * 根据压缩码获取枚举
     * 
     * @param code 压缩码（如 "pv", "ck"）
     * @return 对应的事件类型枚举，未匹配时返回 CUSTOM
     */
    public static EventTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            return CUSTOM;
        }
        String trimmed = code.trim();
        for (EventTypeEnum type : values()) {
            if (type.code.equals(trimmed)) {
                return type;
            }
        }
        return CUSTOM;
    }
    
    /**
     * 获取完整事件名
     * 
     * @return 完整事件名（如 "pageview", "click"）
     */
    public String getEventName() {
        switch (this) {
            case PAGEVIEW: return "pageview";
            case CLICK: return "click";
            case PERFORMANCE: return "performance";
            case ERROR: return "error";
            default: return "custom";
        }
    }
}


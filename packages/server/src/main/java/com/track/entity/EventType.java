package com.track.entity;

/**
 * 事件类型枚举
 * 注意：此枚举值与客户端必须完全一致，修改时需同步更新客户端
 */
public enum EventType {
  PAGE_VIEW(1, "page_view", "页面浏览"),
  CLICK(2, "click", "点击事件"),
  PERFORMANCE(3, "performance", "性能指标"),
  ERROR(4, "error", "错误监控"),
  CUSTOM(5, "custom", "自定义事件"),
  PAGE_STAY(6, "page_stay", "页面停留");

  private final int code;
  private final String name;
  private final String description;

  EventType(int code, String name, String description) {
    this.code = code;
    this.name = name;
    this.description = description;
  }

  public int getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  /**
   * 根据 code 获取枚举值
   * 
   * @param code 事件类型代码
   * @return EventType 枚举值
   * @throws IllegalArgumentException 如果 code 不存在
   */
  public static EventType fromCode(int code) {
    for (EventType type : values()) {
      if (type.code == code) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown event type code: " + code);
  }
}

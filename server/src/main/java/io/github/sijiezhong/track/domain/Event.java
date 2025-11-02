package io.github.sijiezhong.track.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 事件表实体（对应 event 表）
 * 字段说明已用简体中文注释
 */
@Entity
@Table(name = "event")
public class Event {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 事件主键ID

  @Column(nullable = false, length = 64)
  private String eventName; // 事件名称

  @Column(name = "user_id")
  private Integer userId; // 关联用户ID，可匿名

  @Column(name = "session_id")
  private Long sessionId; // 关联的会话ID

  @Column(nullable = false, columnDefinition = "text")
  private String properties; // 事件属性(JSON字符串)

  private Integer tenantId; // 多租户ID

  // 结构化补充字段
  @Column(length = 512)
  private String ua; // User-Agent 原文

  @Column(length = 1024)
  private String referrer; // 来源页

  @Column(length = 64)
  private String ip; // 客户端IP

  @Column(length = 64)
  private String device; // 设备类型（Desktop/Mobile/Tablet/Other）

  @Column(length = 128)
  private String os; // 操作系统

  @Column(length = 128)
  private String browser; // 浏览器名称

  @Column(length = 64)
  private String channel; // 渠道（可选，从上报/推导）

  @Column(name = "anonymous_id", length = 128)
  private String anonymousId; // 匿名ID（可选）

  @Column(nullable = false)
  private LocalDateTime eventTime = LocalDateTime.now(); // 事件时间

  @Column(nullable = false)
  private LocalDateTime createTime = LocalDateTime.now(); // 记录创建时间

  @Column(nullable = false)
  private LocalDateTime updateTime = LocalDateTime.now(); // 记录更新时间

  public Event() {
  }

  // getter/setter 下面已全部补全

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public void setSessionId(Long sessionId) {
    this.sessionId = sessionId;
  }

  public String getProperties() {
    return properties;
  }

  public void setProperties(String properties) {
    this.properties = properties;
  }

  public Integer getTenantId() {
    return tenantId;
  }

  public void setTenantId(Integer tenantId) {
    this.tenantId = tenantId;
  }

  public String getUa() { return ua; }
  public void setUa(String ua) { this.ua = ua; }

  public String getReferrer() { return referrer; }
  public void setReferrer(String referrer) { this.referrer = referrer; }

  public String getIp() { return ip; }
  public void setIp(String ip) { this.ip = ip; }

  public String getDevice() { return device; }
  public void setDevice(String device) { this.device = device; }

  public String getOs() { return os; }
  public void setOs(String os) { this.os = os; }

  public String getBrowser() { return browser; }
  public void setBrowser(String browser) { this.browser = browser; }

  public String getChannel() { return channel; }
  public void setChannel(String channel) { this.channel = channel; }

  public String getAnonymousId() { return anonymousId; }
  public void setAnonymousId(String anonymousId) { this.anonymousId = anonymousId; }

  public LocalDateTime getEventTime() {
    return eventTime;
  }

  public void setEventTime(LocalDateTime eventTime) {
    this.eventTime = eventTime;
  }

  public LocalDateTime getCreateTime() {
    return createTime;
  }

  public void setCreateTime(LocalDateTime createTime) {
    this.createTime = createTime;
  }

  public LocalDateTime getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(LocalDateTime updateTime) {
    this.updateTime = updateTime;
  }

  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (this.createTime == null) this.createTime = now;
    if (this.eventTime == null) this.eventTime = now;
    this.updateTime = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updateTime = LocalDateTime.now();
  }
}
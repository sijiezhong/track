package io.github.sijiezhong.track.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 会话表实体（对应 session 表）
 */
@Entity
@Table(name = "session")
public class Session {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id; // 会话主键ID

  @Column(nullable = false, length = 64, unique = true)
  private String sessionId; // 会话ID，唯一标识本次会话

  @Column(name = "user_id")
  private Integer userId; // 关联用户ID（可匿名）

  @Column(length = 255)
  private String userAgent; // 设备或浏览器 UA

  @Column(length = 64)
  private String ip; // 来源IP

  private Integer tenantId; // 多租户ID

  @Column(nullable = false)
  private LocalDateTime startTime = LocalDateTime.now(); // 会话开始时间

  @Column(nullable = false)
  private LocalDateTime endTime = LocalDateTime.now(); // 会话结束/活动截止时间

  @Column(nullable = false)
  private LocalDateTime createTime = LocalDateTime.now(); // 记录创建时间

  @Column(nullable = false)
  private LocalDateTime updateTime = LocalDateTime.now(); // 记录更新时间

  // 必须的无参构造方法
  public Session() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public Integer getUserId() {
    return userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public Integer getTenantId() {
    return tenantId;
  }

  public void setTenantId(Integer tenantId) {
    this.tenantId = tenantId;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
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
    if (this.createTime == null) {
      this.createTime = now;
    }
    this.updateTime = now;
    if (this.startTime == null) {
      this.startTime = now;
    }
    if (this.endTime == null) {
      this.endTime = now;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.updateTime = LocalDateTime.now();
  }
}
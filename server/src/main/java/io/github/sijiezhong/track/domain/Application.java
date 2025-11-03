package io.github.sijiezhong.track.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 应用表实体（对应 application 表）
 * 字段说明已用简体中文注释
 */
@Entity
@Table(name = "application")
public class Application {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id; // 应用主键

  @Column(nullable = false, length = 64, unique = true)
  private String appKey; // app key，唯一

  @Column(nullable = false, length = 128)
  private String appName; // 应用名称

  private Integer ownerId; // 所属用户ID（开发者/管理员）

  private Integer appId; // 应用ID（注：本表自身就是应用表，此字段用于关联父应用或分组）

  @Column(nullable = false)
  private LocalDateTime createTime = LocalDateTime.now(); // 创建时间

  @Column(nullable = false)
  private LocalDateTime updateTime = LocalDateTime.now(); // 更新时间

  public Application() {
  }

  // getter/setter

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getAppKey() {
    return appKey;
  }

  public void setAppKey(String appKey) {
    this.appKey = appKey;
  }

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public Integer getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(Integer ownerId) {
    this.ownerId = ownerId;
  }

  public Integer getAppId() {
    return appId;
  }

  public void setAppId(Integer appId) {
    this.appId = appId;
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
  }

  @PreUpdate
  protected void onUpdate() {
    this.updateTime = LocalDateTime.now();
  }
}
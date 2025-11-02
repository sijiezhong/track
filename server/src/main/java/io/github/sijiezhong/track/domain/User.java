package io.github.sijiezhong.track.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户表实体（对应 user 表）
 * 字段解释已用简体中文注释
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 用户主键ID

    @Column(nullable = false, length = 64, unique = true)
    private String username; // 用户名，唯一

    @Column(nullable = false, length = 256)
    private String password; // 密码（Hash保存）

    @Column(length = 64)
    private String realName; // 真实姓名

    @Column(length = 128)
    private String email; // 邮箱

    @Column(length = 32)
    private String phone; // 手机号

    @Column(nullable = false)
    private Boolean isAnonymous = false; // 是否匿名用户

    private Integer tenantId; // 多租户ID

    @Column(nullable = false)
    private LocalDateTime createTime = LocalDateTime.now(); // 创建时间

    @Column(nullable = false)
    private LocalDateTime updateTime = LocalDateTime.now(); // 更新时间

    public User() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getIsAnonymous() {
        return isAnonymous;
    }

    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
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
package io.github.sijiezhong.track.dto;

import io.github.sijiezhong.track.domain.User;

/**
 * 登录响应DTO
 * 
 * @author sijie
 */
public class LoginResponse {
    
    private String token;
    private User user;
    private String role;
    private Integer appId;
    
    public LoginResponse() {}
    
    public LoginResponse(String token, User user, String role, Integer appId) {
        this.token = token;
        this.user = user;
        this.role = role;
        this.appId = appId;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
    
    public Integer getTenantId() {
        return appId;
    }
    
    public void setTenantId(Integer appId) {
        this.appId = appId;
    }
}


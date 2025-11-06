package com.track.controller;

import com.track.dto.SessionRequest;
import com.track.dto.SessionResponse;
import com.track.dto.SessionInfo;
import com.track.dto.ErrorResponse;
import com.track.entity.Project;
import com.track.repository.ProjectRepository;
import com.track.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Session 管理 Controller
 */
@RestController
@RequestMapping("/api/session")
@Tag(name = "Session", description = "会话管理接口")
public class SessionController {
    
    private final SessionService sessionService;
    private final ProjectRepository projectRepository;
    
    @Autowired
    public SessionController(SessionService sessionService, ProjectRepository projectRepository) {
        this.sessionService = sessionService;
        this.projectRepository = projectRepository;
    }
    
    @PostMapping
    @Operation(summary = "注册用户会话", description = "创建新的 session 并设置 Cookie")
    public ResponseEntity<?> createSession(
            @RequestBody SessionRequest request,
            HttpServletResponse response) {
        
        // 验证 / 初始化 AppId
        Optional<Project> projectOpt = projectRepository.findByAppId(request.getAppId());
        if (projectOpt.isEmpty()) {
            // 不存在则自动创建项目（appName 为空时用 appId 作为项目名）
            Project newProject = new Project();
            newProject.setAppId(request.getAppId());
            newProject.setAppName(request.getAppName() != null && !request.getAppName().isEmpty()
                    ? request.getAppName()
                    : request.getAppId());
            newProject.setIsActive(true);
            projectRepository.save(newProject);
        } else if (!projectOpt.get().getIsActive()) {
            // 已存在但未激活
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INACTIVE_PROJECT", "项目已被禁用，请联系管理员启用"));
        }
        
        // 生成 sessionId
        String sessionId = UUID.randomUUID().toString();
        
        // 保存到 Redis（TTL 由请求参数决定，默认 24 小时）
        int ttlMinutes = request.getTtlMinutes() != null ? request.getTtlMinutes() : 1440;
        sessionService.saveSession(sessionId, request.getAppId(), 
            request.getUserId(), request.getUserProps(), ttlMinutes);
        
        // 设置 Cookie
        Cookie cookie = new Cookie("track_session_id", sessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 开发环境，生产环境应设为 true
        cookie.setMaxAge(ttlMinutes == 0 ? Integer.MAX_VALUE : ttlMinutes * 60);
        response.addCookie(cookie);
        
        return ResponseEntity.ok(new SessionResponse(sessionId));
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "刷新会话", description = "延长 session 过期时间")
    public ResponseEntity<Void> refreshSession(
            @CookieValue(value = "track_session_id", required = false) String sessionId,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        if (sessionId == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }
        
        SessionInfo sessionInfo = sessionService.getSession(sessionId);
        if (sessionInfo == null) {
            return ResponseEntity.status(401).build(); // Session 不存在或已过期
        }
        
        // 刷新 Redis TTL（使用原始 TTL）
        sessionService.refreshSession(sessionId, sessionInfo.getTtlMinutes());
        
        // 刷新 Cookie
        Cookie cookie = new Cookie("track_session_id", sessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // 开发环境，生产环境应设为 true
        cookie.setMaxAge(sessionInfo.getTtlMinutes() == 0 ? Integer.MAX_VALUE : 
            sessionInfo.getTtlMinutes() * 60);
        response.addCookie(cookie);
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/destroy")
    @Operation(summary = "销毁会话", description = "删除 session 并清除 Cookie")
    public ResponseEntity<Void> destroySession(
            @CookieValue(value = "track_session_id", required = false) String sessionId,
            HttpServletResponse response) {
        
        if (sessionId != null) {
            // 删除 Redis 中的 session
            sessionService.deleteSession(sessionId);
        }
        
        // 清除 Cookie
        Cookie cookie = new Cookie("track_session_id", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        
        return ResponseEntity.ok().build();
    }
}


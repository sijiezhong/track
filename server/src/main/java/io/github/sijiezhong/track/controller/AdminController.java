package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.constants.ApiConstants;
import io.github.sijiezhong.track.constants.HttpHeaderConstants;
import io.github.sijiezhong.track.domain.Application;
import io.github.sijiezhong.track.domain.AuditLog;
import io.github.sijiezhong.track.domain.User;
import io.github.sijiezhong.track.dto.ApiResponse;
import io.github.sijiezhong.track.exception.ErrorCode;
import io.github.sijiezhong.track.exception.ForbiddenException;
import io.github.sijiezhong.track.repository.ApplicationRepository;
import io.github.sijiezhong.track.repository.AuditLogRepository;
import io.github.sijiezhong.track.repository.UserRepository;
import io.github.sijiezhong.track.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理控制器
 * 
 * <p>提供应用和用户的管理接口，包括创建和查询功能。
 * 所有操作都会进行应用隔离和审计日志记录。
 * 
 * @author sijie
 */
@RestController
@RequestMapping(ApiConstants.API_PREFIX + "/admin")
@Tag(name = "Admin", description = "应用与用户管理（应用隔离）")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminController(ApplicationRepository applicationRepository, UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * 创建应用
     * 
     * @param appId 应用ID请求头（必填）
     * @param app 应用信息
     * @return 创建的应用
     */
    @PostMapping("/apps")
    @Operation(summary = "创建应用")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Application>> createApp(
            @Parameter(description = "应用头，必填") @RequestHeader(HttpHeaderConstants.HEADER_APP_ID) Integer appId,
            @RequestBody Application app) {
        
        log.info("创建应用请求: appId={}, appName={}", appId, app.getAppName());
        
        // 应用ID校验
        if (app.getAppId() != null && !app.getAppId().equals(appId)) {
            throw new ForbiddenException(ErrorCode.APP_ID_MISMATCH);
        }
        if (app.getAppId() == null) {
            app.setAppId(appId);
        }
        
        Application saved = applicationRepository.save(app);
        
        log.info("应用创建成功: appId={}, appId={}", saved.getId(), saved.getAppId());
        
        // 记录审计日志
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setAppId(appId);
            logEntry.setUsername("admin");
            logEntry.setMethod("POST");
            logEntry.setPath("/api/v1/admin/apps");
            logEntry.setPayload("{}");
            logEntry.setCreateTime(LocalDateTime.now());
            auditLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("审计日志记录失败", e);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseUtil.success(saved));
    }

    /**
     * 按应用查询应用列表
     * 
     * @param appId 应用ID请求头（必填）
     * @return 应用列表
     */
    @GetMapping("/apps")
    @Operation(summary = "按应用查询应用")
    public ApiResponse<List<Application>> listApps(@RequestHeader(HttpHeaderConstants.HEADER_APP_ID) Integer appId) {
        log.debug("查询应用列表: appId={}", appId);
        
        List<Application> apps = applicationRepository.findByAppId(appId);
        
        log.debug("应用列表查询完成: appId={}, count={}", appId, apps.size());
        
        return ResponseUtil.success(apps);
    }

    /**
     * 创建用户
     * 
     * @param appId 应用ID请求头（必填）
     * @param user 用户信息
     * @return 创建的用户
     */
    @PostMapping("/users")
    @Operation(summary = "创建用户")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> createUser(
            @Parameter(description = "应用头，必填") @RequestHeader(HttpHeaderConstants.HEADER_APP_ID) Integer appId,
            @RequestBody User user) {
        
        log.info("创建用户请求: appId={}, username={}", appId, user.getUsername());
        
        // 应用ID校验
        if (user.getAppId() != null && !user.getAppId().equals(appId)) {
            throw new ForbiddenException(ErrorCode.APP_ID_MISMATCH);
        }
        if (user.getAppId() == null) {
            user.setAppId(appId);
        }
        
        User saved = userRepository.save(user);
        
        log.info("用户创建成功: userId={}, appId={}", saved.getId(), saved.getAppId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseUtil.success(saved));
    }

    /**
     * 按应用查询用户列表
     * 
     * @param appId 应用ID请求头（必填）
     * @return 用户列表
     */
    @GetMapping("/users")
    @Operation(summary = "按应用查询用户")
    public ApiResponse<List<User>> listUsers(@RequestHeader(HttpHeaderConstants.HEADER_APP_ID) Integer appId) {
        log.debug("查询用户列表: appId={}", appId);
        
        List<User> users = userRepository.findByAppId(appId);
        
        log.debug("用户列表查询完成: appId={}, count={}", appId, users.size());
        
        return ResponseUtil.success(users);
    }
}

package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.constants.ApiConstants;
import io.github.sijiezhong.track.domain.User;
import io.github.sijiezhong.track.dto.ApiResponse;
import io.github.sijiezhong.track.dto.LoginRequest;
import io.github.sijiezhong.track.dto.LoginResponse;
import io.github.sijiezhong.track.exception.BusinessException;
import io.github.sijiezhong.track.exception.ErrorCode;
import io.github.sijiezhong.track.repository.UserRepository;
import io.github.sijiezhong.track.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 认证控制器
 * 
 * <p>提供用户认证相关接口，包括登录、登出等功能。
 * 
 * @author sijie
 */
@RestController
@RequestMapping(ApiConstants.API_PREFIX + "/auth")
@Tag(name = "Authentication", description = "用户认证接口")
public class AuthController {
    
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    private final UserRepository userRepository;
    
    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * 用户登录
     * 
     * @param request 登录请求
     * @return 登录响应（包含token和用户信息）
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "验证用户名和密码，返回用户信息和token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("收到登录请求: username={}", request.getUsername());
        
        // 查询用户
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            log.warn("用户不存在: username={}", request.getUsername());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        
        User user = userOpt.get();
        
        // 验证密码（这里简化处理，实际应该使用加密比对）
        // TODO: 使用 BCrypt 或其他加密算法验证密码
        if (!request.getPassword().equals(user.getPassword())) {
            log.warn("密码错误: username={}", request.getUsername());
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        
        // 生成token（简化处理，格式为 role:ADMIN）
        // 这里根据用户的实际角色生成token，默认使用ADMIN
        // TODO: 实现真实的JWT token生成逻辑
        String role = determineUserRole(user);
        String token = "role:" + role;
        
        // 构造响应
        LoginResponse response = new LoginResponse(
            token,
            user,
            role,
            user.getAppId()
        );
        
        log.info("登录成功: username={}, appId={}, role={}", 
            request.getUsername(), user.getAppId(), role);
        
        return ResponseEntity.ok(ResponseUtil.success(response));
    }
    
    /**
     * 确定用户角色
     * 
     * @param user 用户对象
     * @return 角色名称
     */
    private String determineUserRole(User user) {
        // 这里简化处理，根据用户ID判断角色
        // 实际应该从数据库的角色表中查询
        if (user.getId() == 1) {
            return "ADMIN";
        }
        return "ANALYST";
    }
}


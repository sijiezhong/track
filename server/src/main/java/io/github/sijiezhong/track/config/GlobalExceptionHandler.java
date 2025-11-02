package io.github.sijiezhong.track.config;

import io.github.sijiezhong.track.dto.ApiError;
import io.github.sijiezhong.track.exception.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 
 * <p>统一处理系统中所有异常，将其转换为统一的错误响应格式。
 * 使用 {@code @RestControllerAdvice} 注解，自动捕获所有Controller抛出的异常。
 * 
 * @author sijie
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理业务异常
     * 
     * @param e 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ApiError error = new ApiError(
            errorCode.getHttpStatus(),
            e.getMessage(),
            errorCode.getCode()
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(error);
    }
    
    /**
     * 处理验证异常
     * 
     * @param e 验证异常
     * @return 错误响应
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(ValidationException e) {
        log.warn("参数验证异常: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ApiError error = new ApiError(
            errorCode.getHttpStatus(),
            e.getMessage(),
            errorCode.getCode(),
            e.getDetails()
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(error);
    }
    
    /**
     * 处理资源未找到异常
     * 
     * @param e 资源未找到异常
     * @return 错误响应
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(ResourceNotFoundException e) {
        log.warn("资源未找到: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ApiError error = new ApiError(
            errorCode.getHttpStatus(),
            e.getMessage(),
            errorCode.getCode()
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(error);
    }
    
    /**
     * 处理未授权异常
     * 
     * @param e 未授权异常
     * @return 错误响应
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorizedException(UnauthorizedException e) {
        log.warn("未授权访问: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ApiError error = new ApiError(
            errorCode.getHttpStatus(),
            e.getMessage(),
            errorCode.getCode()
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(error);
    }
    
    /**
     * 处理禁止访问异常
     * 
     * @param e 禁止访问异常
     * @return 错误响应
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbiddenException(ForbiddenException e) {
        log.warn("禁止访问: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ApiError error = new ApiError(
            errorCode.getHttpStatus(),
            e.getMessage(),
            errorCode.getCode()
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(error);
    }
    
    /**
     * 处理Spring Validation参数验证异常
     * 
     * @param e 方法参数验证异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());
        
        log.warn("参数验证失败: {}", details);
        
        ApiError error = new ApiError(
            ErrorCode.VALIDATION_ERROR.getHttpStatus(),
            ErrorCode.VALIDATION_ERROR.getMessage(),
            ErrorCode.VALIDATION_ERROR.getCode(),
            details
        );
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus()).body(error);
    }
    
    /**
     * 处理ConstraintViolationException（如@Valid在方法参数上的验证失败）
     * 
     * @param e 约束违反异常
     * @return 错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(ConstraintViolationException e) {
        List<String> details = e.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.toList());
        
        log.warn("约束验证失败: {}", details);
        
        ApiError error = new ApiError(
            ErrorCode.VALIDATION_ERROR.getHttpStatus(),
            ErrorCode.VALIDATION_ERROR.getMessage(),
            ErrorCode.VALIDATION_ERROR.getCode(),
            details
        );
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus()).body(error);
    }
    
    /**
     * 处理方法参数类型不匹配异常
     * 
     * @param e 方法参数类型不匹配异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String typeName = "unknown";
        if (e.getRequiredType() != null) {
            typeName = e.getRequiredType().getSimpleName();
        }
        String message = String.format("参数 '%s' 类型不匹配，期望类型: %s", 
            e.getName(), 
            typeName);
        
        log.warn("参数类型不匹配: {}", message);
        
        ApiError error = new ApiError(
            ErrorCode.BAD_REQUEST.getHttpStatus(),
            message,
            ErrorCode.BAD_REQUEST.getCode()
        );
        return ResponseEntity.status(ErrorCode.BAD_REQUEST.getHttpStatus()).body(error);
    }
    
    /**
     * 处理缺少必需请求头异常
     * 
     * <p>对于X-Tenant-Id请求头缺失，根据路径判断返回403还是400：
     * - 如果TenantGuardInterceptor已经处理过（设置403），保持403
     * - 如果TenantGuardInterceptor没有启用，根据路径判断：
     *   - 受保护的敏感路径（admin、webhooks、analytics、export）返回403
     *   - 其他路径返回400（因为Controller会抛出MissingRequestHeaderException）
     * 
     * @param e 缺少请求头异常
     * @return 错误响应
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        String headerName = e.getHeaderName();
        String requestPath = getRequestPath(e);
        
        // 对于X-Tenant-Id请求头缺失的处理
        if ("X-Tenant-Id".equals(headerName)) {
            // 如果是分析接口路径（有@PreAuthorize），检查认证状态
            // 注意：分析接口路径如 /api/v1/events/segmentation 需要先检查认证
            if (isAnalyticsPath(requestPath)) {
                // 检查Security是否启用（如果Security被禁用，认证上下文可能为null，但不应该返回401）
                try {
                    org.springframework.security.core.context.SecurityContext context = 
                        org.springframework.security.core.context.SecurityContextHolder.getContext();
                    if (context != null) {
                        org.springframework.security.core.Authentication auth = context.getAuthentication();
                        boolean isUnauthenticated = auth == null || !auth.isAuthenticated() || 
                            auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;
                        
                        // 只有在Security启用且确实未认证时才返回401
                        // 如果Security被禁用，应该返回400（缺少请求头）
                        if (isUnauthenticated && isSecurityEnabled()) {
                            // 未认证，返回401（即使有租户头，分析接口也需要先认证）
                            String message = "认证失败：缺少认证信息";
                            log.warn("缺少认证信息（分析接口）: {}", requestPath);
                            
                            ApiError error = new ApiError(
                                HttpStatus.UNAUTHORIZED.value(),
                                message,
                                ErrorCode.UNAUTHORIZED.getCode()
                            );
                            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
                        }
                    }
                } catch (Exception ignored) {
                    // 如果Security未启用或发生异常，继续返回400
                }
            }
            
            // 如果是受保护路径（admin、webhooks、export等），返回403
            if (isProtectedPath(requestPath)) {
                String message = "缺少必需的租户ID请求头";
                log.warn("缺少租户ID请求头（受保护路径）: {}", requestPath);
                
                ApiError error = new ApiError(
                    HttpStatus.FORBIDDEN.value(),
                    message,
                    ErrorCode.FORBIDDEN.getCode()
                );
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            // 其他路径（如查询接口），返回400
            String message = "缺少必需的租户ID请求头";
            log.warn("缺少租户ID请求头: {}", requestPath);
            
            ApiError error = new ApiError(
                ErrorCode.BAD_REQUEST.getHttpStatus(),
                message,
                ErrorCode.BAD_REQUEST.getCode()
            );
            return ResponseEntity.status(ErrorCode.BAD_REQUEST.getHttpStatus()).body(error);
        }
        
        // 其他请求头缺失，返回400
        String message = String.format("缺少必需的请求头: %s", headerName);
        log.warn("缺少请求头: {}", message);
        
        ApiError error = new ApiError(
            ErrorCode.BAD_REQUEST.getHttpStatus(),
            message,
            ErrorCode.BAD_REQUEST.getCode()
        );
        return ResponseEntity.status(ErrorCode.BAD_REQUEST.getHttpStatus()).body(error);
    }
    
    /**
     * 从当前请求上下文获取请求路径
     */
    private String getRequestPath(MissingRequestHeaderException e) {
        try {
            org.springframework.web.context.request.RequestAttributes requestAttributes = 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                jakarta.servlet.http.HttpServletRequest request = 
                    ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes).getRequest();
                return request != null ? request.getRequestURI() : "";
            }
        } catch (Exception ignored) {
            // 如果无法获取，返回空字符串
        }
        return "";
    }
    
    /**
     * 判断路径是否为分析接口路径
     */
    private boolean isAnalyticsPath(String path) {
        if (path == null) return false;
        return path.startsWith("/api/v1/events/trend") || path.startsWith("/api/v1/events/path")
            || path.startsWith("/api/v1/events/retention") || path.startsWith("/api/v1/events/funnel")
            || path.startsWith("/api/v1/events/segmentation") || path.startsWith("/api/v1/events/heatmap");
            // 注意：旧路径 /api/analytics 由TenantGuardInterceptor处理，不在这里
    }
    
    /**
     * 检查Security是否启用
     * 
     * <p>通过检查ApplicationContext中是否存在SecurityFilterChain Bean来判断。
     * 如果测试中排除了SecurityAutoConfiguration，则SecurityFilterChain不会存在。
     */
    private boolean isSecurityEnabled() {
        try {
            org.springframework.web.context.request.RequestAttributes requestAttributes = 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (requestAttributes != null) {
                org.springframework.context.ApplicationContext appContext = 
                    org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(
                        (jakarta.servlet.ServletContext) null);
                if (appContext != null) {
                    try {
                        // 尝试获取SecurityFilterChain Bean，如果存在说明Security启用
                        appContext.getBean(org.springframework.security.web.SecurityFilterChain.class);
                        return true;
                    } catch (org.springframework.beans.factory.NoSuchBeanDefinitionException ignored) {
                        // 如果找不到SecurityFilterChain，说明Security被禁用
                        return false;
                    } catch (Exception ignored) {
                        // 其他异常，保守处理返回false
                        return false;
                    }
                }
            }
        } catch (Exception ignored) {
            // 如果发生异常，假设Security未启用（保守处理）
        }
        return false;
    }
    
    /**
     * 判断路径是否为受保护路径（需要租户ID，缺少时返回403）
     * 
     * <p>注意：分析接口和查询接口不在受保护路径中，因为它们有自己的验证逻辑。
     */
    private boolean isProtectedPath(String path) {
        if (path == null) return false;
        // 受保护的路径：管理员、webhooks、导出接口、流接口
        // 注意：分析接口不在受保护路径中，因为它们需要先检查认证（返回401）
        // 注意：查询接口不在受保护路径中，因为它返回400而不是403
        return path.startsWith("/api/v1/admin") || path.startsWith("/api/v1/webhooks")
            || path.startsWith("/api/v1/events/export") || path.startsWith("/api/v1/events/stream")
            // 向后兼容旧路径
            || path.startsWith("/api/admin") || path.startsWith("/api/events/export") 
            || path.startsWith("/api/webhook");
    }
    
    /**
     * 处理路径未找到异常（404）
     * 
     * <p>注意：如果路径是受保护路径但没有租户头，TenantGuardInterceptor应该已经拦截返回403。
     * 如果到达这里，可能是TenantGuardInterceptor未启用，或者路径匹配有问题。
     * 我们检查路径是否应该是受保护的，如果是但没有租户头，返回403。
     * 
     * @param e 路径未找到异常
     * @return 错误响应
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandlerFoundException(NoHandlerFoundException e) {
        String path = e.getRequestURL() != null ? e.getRequestURL().toString() : "";
        log.warn("路径未找到: {}", path);
        
        // 检查响应是否已经被设置为403（可能是TenantGuardInterceptor设置的）
        try {
            org.springframework.web.context.request.RequestAttributes requestAttributes = 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                jakarta.servlet.http.HttpServletResponse response = 
                    ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes).getResponse();
                if (response != null && response.getStatus() == HttpStatus.FORBIDDEN.value()) {
                    // 响应已经被设置为403，不再处理，返回null让框架使用已设置的响应
                    return null;
                }
            }
        } catch (Exception ignored) {
            // 如果无法获取响应，继续处理
        }
        
        // 特殊处理：如果路径看起来像是受保护路径，检查是否有租户头
        // 这用于处理TenantGuardInterceptor未启用或者拦截器没有正确拦截的情况
        // 注意：这里需要检查所有受保护路径，包括旧路径 /api/analytics 的所有子路径
        if (isProtectedPath(path) || (path != null && path.startsWith("/api/analytics"))) {
            try {
                org.springframework.web.context.request.RequestAttributes requestAttributes = 
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
                if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                    jakarta.servlet.http.HttpServletRequest request = 
                        ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes).getRequest();
                    String tenantHeader = request.getHeader("X-Tenant-Id");
                    if (tenantHeader == null || tenantHeader.isBlank()) {
                        // 受保护路径但没有租户头，返回403
                        // 即使TenantGuardInterceptor已经设置了403状态码，我们也确保返回403响应
                        log.warn("受保护路径缺少租户ID请求头: {}", path);
                        ApiError error = new ApiError(
                            ErrorCode.FORBIDDEN.getHttpStatus(),
                            "缺少必需的租户ID请求头",
                            ErrorCode.FORBIDDEN.getCode()
                        );
                        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus()).body(error);
                    }
                }
            } catch (Exception ignored) {
                // 如果无法获取请求信息，继续返回404
            }
        }
        
        ApiError error = new ApiError(
            ErrorCode.NOT_FOUND.getHttpStatus(),
            "路径未找到: " + path,
            ErrorCode.NOT_FOUND.getCode()
        );
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getHttpStatus()).body(error);
    }
    
    /**
     * 处理访问被拒绝异常（403）
     * 
     * <p>注意：在secure profile下，如果未认证，Spring Security的accessDeniedHandler应该已经处理并返回401。
     * 但如果到达这里，可能是由于某些特殊情况，我们检查认证状态以确保正确返回401或403。
     * 
     * @param e 访问被拒绝异常
     * @return 错误响应
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("访问被拒绝: {}", e.getMessage());
        
        // 检查认证状态：如果未认证，应该返回401而不是403
        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || 
            auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            // 未认证，返回401
            log.warn("访问被拒绝，但用户未认证，返回401");
            ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "认证失败：缺少认证信息",
                ErrorCode.UNAUTHORIZED.getCode()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        // 已认证但权限不足，返回403
        ApiError error = new ApiError(
            ErrorCode.FORBIDDEN.getHttpStatus(),
            "访问被拒绝",
            ErrorCode.FORBIDDEN.getCode()
        );
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus()).body(error);
    }
    
    /**
     * 处理认证异常（401）
     * 
     * @param e 认证异常
     * @return 错误响应
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        
        ApiError error = new ApiError(
            ErrorCode.UNAUTHORIZED.getHttpStatus(),
            "认证失败",
            ErrorCode.UNAUTHORIZED.getCode()
        );
        return ResponseEntity.status(ErrorCode.UNAUTHORIZED.getHttpStatus()).body(error);
    }
    
    /**
     * 处理ResponseStatusException（保持向后兼容）
     * 
     * @param e 响应状态异常
     * @return 错误响应
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException e) {
        org.springframework.http.HttpStatusCode statusCode = e.getStatusCode();
        String reason = e.getReason();
        
        // 将HttpStatusCode转换为HttpStatus
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        log.warn("响应状态异常: {} - {}", status, reason);
        
        // 尝试从HttpStatus映射到ErrorCode
        ErrorCode errorCode = mapHttpStatusToErrorCode(status);
        ApiError error = new ApiError(
            status.value(),
            reason != null ? reason : errorCode.getMessage(),
            errorCode.getCode()
        );
        return ResponseEntity.status(status).body(error);
    }
    
    /**
     * 处理其他未预期的异常
     * 
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception e) {
        log.error("未预期的异常", e);
        
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        ApiError error = new ApiError(
            errorCode.getHttpStatus(),
            errorCode.getMessage(),
            errorCode.getCode()
        );
        return ResponseEntity.status(errorCode.getHttpStatus()).body(error);
    }
    
    /**
     * 将HttpStatus映射到ErrorCode
     * 
     * @param status HTTP状态码
     * @return 对应的ErrorCode
     */
    private ErrorCode mapHttpStatusToErrorCode(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> ErrorCode.BAD_REQUEST;
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> ErrorCode.FORBIDDEN;
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> ErrorCode.METHOD_NOT_ALLOWED;
            case CONFLICT -> ErrorCode.CONFLICT;
            case INTERNAL_SERVER_ERROR -> ErrorCode.INTERNAL_ERROR;
            case SERVICE_UNAVAILABLE -> ErrorCode.SERVICE_UNAVAILABLE;
            default -> ErrorCode.INTERNAL_ERROR;
        };
    }
}


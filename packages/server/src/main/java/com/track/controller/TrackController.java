package com.track.controller;

import com.track.dto.SessionInfo;
import com.track.dto.TrackBatchRequest;
import com.track.service.RateLimitService;
import com.track.service.SessionService;
import com.track.service.TrackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 事件采集 Controller
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Track", description = "事件采集接口")
public class TrackController {
    
    private static final Logger log = LoggerFactory.getLogger(TrackController.class);
    
    private final TrackService trackService;
    private final SessionService sessionService;
    private final RateLimitService rateLimitService;
    
    @Autowired
    public TrackController(TrackService trackService, SessionService sessionService, 
                          RateLimitService rateLimitService) {
        this.trackService = trackService;
        this.sessionService = sessionService;
        this.rateLimitService = rateLimitService;
    }
    
    @PostMapping("/ingest")
    @Operation(summary = "接收埋点数据", description = "通过POST请求接收埋点数据，需要CORS支持")
    public ResponseEntity<Void> trackEvent(
            @RequestBody TrackBatchRequest batchRequest,
            @CookieValue(value = "track_session_id", required = false) String sessionId,
            HttpServletRequest request) {
        
        try {
            // 1. 从 Cookie 获取 sessionId
            if (sessionId == null) {
                return ResponseEntity.status(401).build(); // Unauthorized
            }
            
            // 2. 从 Redis 获取用户信息
            SessionInfo sessionInfo = sessionService.getSession(sessionId);
            if (sessionInfo == null) {
                return ResponseEntity.status(401).build(); // Session 不存在或已过期
            }
            
            // 3. 验证 AppId（单公司多项目：验证项目是否存在且激活）
            if (!trackService.validateAppId(sessionInfo.getAppId())) {
                return ResponseEntity.badRequest().build();
            }
            
            // 4. 限流检查
            String clientIp = getClientIp(request);
            if (!rateLimitService.isAllowed(sessionInfo.getAppId(), clientIp)) {
                return ResponseEntity.status(429).build(); // Too Many Requests
            }
            
            // 5. 补充信息（从 session 和请求中获取）
            batchRequest.setAppId(sessionInfo.getAppId());
            batchRequest.setUserId(sessionInfo.getUserId());
            batchRequest.setUserProps(sessionInfo.getUserProps());
            batchRequest.setServerTimestamp(LocalDateTime.now());
            batchRequest.setIpAddress(clientIp);
            batchRequest.setUserAgent(request.getHeader("User-Agent"));
            
            // 6. 异步处理（不阻塞响应）
            trackService.processBatchEventsAsync(batchRequest);
            
            return ResponseEntity.accepted().build(); // 202 Accepted
            
        } catch (Exception e) {
            log.error("Track event processing error", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 获取客户端 IP 地址
     * 优先从 X-Forwarded-For 头获取（支持代理场景）
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}


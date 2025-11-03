package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.constants.ApiConstants;
import io.github.sijiezhong.track.constants.HttpHeaderConstants;
import io.github.sijiezhong.track.domain.WebhookSubscription;
import io.github.sijiezhong.track.dto.ApiResponse;
import io.github.sijiezhong.track.exception.ErrorCode;
import io.github.sijiezhong.track.exception.ForbiddenException;
import io.github.sijiezhong.track.repository.WebhookSubscriptionRepository;
import io.github.sijiezhong.track.service.WebhookService;
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

/**
 * Webhook管理控制器
 * 
 * <p>提供Webhook订阅的创建和管理功能，以及事件重放功能。
 * 所有操作都会进行应用隔离和权限验证。
 * 
 * @author sijie
 */
@RestController
@RequestMapping(ApiConstants.API_PREFIX + "/webhooks")
@Tag(name = "Webhooks", description = "Webhook 订阅管理与重放")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WebhookSubscriptionRepository repository;
    private final WebhookService webhookService;

    public WebhookController(WebhookSubscriptionRepository repository, WebhookService webhookService) {
        this.repository = repository;
        this.webhookService = webhookService;
    }

    /**
     * 创建Webhook订阅
     * 
     * @param appId 应用ID请求头（必填）
     * @param sub Webhook订阅信息
     * @return 创建的Webhook订阅
     */
    @PostMapping
    @Operation(summary = "创建订阅")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WebhookSubscription>> create(
            @Parameter(description = "应用头，必填") @RequestHeader(HttpHeaderConstants.HEADER_APP_ID) Integer appId,
            @RequestBody WebhookSubscription sub) {
        
        log.info("创建Webhook订阅请求: appId={}, url={}", appId, sub.getUrl());
        
        // 应用ID校验
        if (sub.getAppId() != null && !sub.getAppId().equals(appId)) {
            throw new ForbiddenException(ErrorCode.APP_ID_MISMATCH);
        }
        sub.setAppId(appId);
        sub.setCreateTime(LocalDateTime.now());
        sub.setUpdateTime(LocalDateTime.now());
        if (sub.getEnabled() == null) {
            sub.setEnabled(Boolean.TRUE);
        }
        
        WebhookSubscription saved = repository.save(sub);
        
        log.info("Webhook订阅创建成功: id={}, appId={}", saved.getId(), saved.getAppId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseUtil.success(saved));
    }

    /**
     * 重放最近一次事件
     * 
     * @param appId 应用ID请求头（必填）
     * @return 操作结果
     */
    @PostMapping("/replay/latest")
    @Operation(summary = "重放最近一次事件")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> replayLatest(@RequestHeader(HttpHeaderConstants.HEADER_APP_ID) Integer appId) {
        log.info("重放最近一次事件请求: appId={}", appId);
        
        webhookService.replayLatest(appId);
        
        log.info("事件重放完成: appId={}", appId);
        
        return ResponseEntity.ok(ResponseUtil.success());
    }
}

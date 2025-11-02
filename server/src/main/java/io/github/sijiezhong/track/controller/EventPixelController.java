package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.constants.HttpHeaderConstants;
import io.github.sijiezhong.track.dto.EventCollectRequest;
import io.github.sijiezhong.track.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 像素上报控制器
 * 
 * <p>
 * 提供1x1像素GIF上报接口，兼容传统的图片追踪方式。
 * 支持通过查询参数传递事件信息，返回1x1透明GIF图片。
 * 
 * @author sijie
 */
@RestController
@Tag(name = "Pixel", description = "1x1像素上报接口")
public class EventPixelController {

    private static final Logger log = LoggerFactory.getLogger(EventPixelController.class);

    /**
     * 1x1透明GIF图片的字节数组（最小GIF格式）
     */
    private static final byte[] ONE_BY_ONE_GIF = new byte[] {
            71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, 0, 0, 0, 0, 0, -1, -1, -1, 33, -7, 4, 1, 0, 0, 0, 0, 44, 0, 0, 0,
            0, 1, 0, 1, 0, 0, 2, 2, 68, 1, 0, 59
    };

    private final EventService eventService;

    public EventPixelController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * 像素上报接口
     * 
     * <p>
     * 返回1x1透明GIF图片并记录事件。支持通过查询参数传递事件信息。
     * 
     * @param tenantId  租户ID请求头（必填）
     * @param eventName 事件名称（默认"pixel"）
     * @param sessionId 会话ID（可选）
     * @param userId    用户ID（可选）
     * @return 1x1透明GIF图片
     */
    @GetMapping(value = "/api/v1/pixel.gif")
    @Operation(summary = "像素上报：返回1x1 GIF 并记录事件")
    public ResponseEntity<byte[]> pixel(
            @Parameter(description = "租户头，必填") @RequestHeader(HttpHeaderConstants.HEADER_TENANT_ID) Integer tenantId,
            @Parameter(description = "事件名") @RequestParam(name = "eventName", required = false, defaultValue = "pixel") String eventName,
            @Parameter(description = "会话ID") @RequestParam(name = "sessionId", required = false) String sessionId,
            @Parameter(description = "用户ID") @RequestParam(name = "userId", required = false) Integer userId) {

        log.debug("收到像素上报请求: tenantId={}, eventName={}, sessionId={}", tenantId, eventName, sessionId);

        EventCollectRequest req = new EventCollectRequest();
        req.setTenantId(tenantId);
        req.setEventName(eventName);
        req.setSessionId(sessionId);
        req.setUserId(userId);
        // 填充最小必需字段
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        req.setProperties(om.createObjectNode());
        // 持久化事件（忽略返回值）
        eventService.save(req);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "image/gif");
        headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic().getHeaderValue());
        return new ResponseEntity<>(ONE_BY_ONE_GIF, headers, HttpStatus.OK);
    }
}

package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.constants.ApiConstants;
import io.github.sijiezhong.track.constants.HttpHeaderConstants;
import io.github.sijiezhong.track.stream.EventStreamBroadcaster;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 事件流控制器
 * 
 * <p>提供Server-Sent Events (SSE)实时事件流推送功能。
 * 客户端可以通过订阅此接口实时接收事件创建通知。
 * 
 * @author sijie
 */
@RestController
@RequestMapping(value = ApiConstants.API_PREFIX + "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@Tag(name = "Event Stream", description = "事件SSE实时流")
public class EventStreamController {

    private static final Logger log = LoggerFactory.getLogger(EventStreamController.class);

    private final EventStreamBroadcaster broadcaster;

    public EventStreamController(EventStreamBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    /**
     * 订阅事件SSE流
     * 
     * <p>按租户维度订阅事件创建实时推送。客户端需要保持HTTP连接开启以接收事件。
     * 
     * @param tenantId 租户ID请求头（必填）
     * @return SSE发射器
     */
    @GetMapping("/stream")
    @Operation(summary = "订阅事件SSE流", description = "按租户维度订阅事件创建实时推送")
    public SseEmitter stream(@Parameter(description = "租户头，必填") @RequestHeader(HttpHeaderConstants.HEADER_TENANT_ID) Integer tenantId) {
        log.info("客户端订阅事件流: tenantId={}", tenantId);
        return broadcaster.subscribe(tenantId);
    }
}

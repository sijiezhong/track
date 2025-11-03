package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.constants.HttpHeaderConstants;
import io.github.sijiezhong.track.constants.EventTypeEnum;
import io.github.sijiezhong.track.dto.EventCollectRequest;
import io.github.sijiezhong.track.dto.PixelBatchEvent;
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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
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
     * 像素上报接口（支持单个和批量事件）
     * 
     * <p>
     * 返回1x1透明GIF图片并记录事件。支持通过查询参数传递事件信息。
     * 为了支持跨域，appId 可以从 URL 参数或请求头获取（优先 URL 参数）。
     * 
     * <p>
     * 支持两种模式：
     * <ul>
     *   <li>单个事件模式：使用 eventName/eventContent 参数（向后兼容）</li>
     *   <li>批量事件模式：使用 eventsB64 参数传递 Base64 编码的批量事件数组</li>
     * </ul>
     * 
     * <p>
     * 参数支持完整名称和缩写：
     * <ul>
     *   <li>appId / t: 应用ID</li>
     *   <li>sessionId / s: 会话ID</li>
     *   <li>userId / u: 用户ID</li>
     *   <li>eventName / n: 事件名（单个模式）</li>
     *   <li>eventContent / c: 事件内容（单个模式）</li>
     *   <li>eventsB64 / b: 批量事件（Base64编码）</li>
     *   <li>batch / bt: 批量模式标识（1=批量，0=单个）</li>
     * </ul>
     * 
     * @param appIdHeader  应用ID请求头（可选）
     * @param appId        应用ID URL参数（完整名，可选）
     * @param appIdShort   应用ID URL参数（缩写 t，可选）
     * @param eventName       事件名称（完整名，单个模式，可选）
     * @param eventNameShort  事件名称（缩写 n，单个模式，可选）
     * @param eventContent    事件内容 JSON（完整名，单个模式，可选）
     * @param eventContentShort 事件内容 JSON（缩写 c，单个模式，可选）
     * @param eventsB64       批量事件 Base64（完整名，批量模式，可选）
     * @param eventsB64Short  批量事件 Base64（缩写 b，批量模式，可选）
     * @param batch           批量模式标识（完整名，可选，默认 "0"）
     * @param batchShort      批量模式标识（缩写 bt，可选）
     * @param sessionId       会话ID（完整名，可选）
     * @param sessionIdShort  会话ID（缩写 s，可选）
     * @param userId          用户ID（完整名，可选）
     * @param userIdShort     用户ID（缩写 u，可选）
     * @return 1x1透明GIF图片
     */
    @GetMapping(value = "/api/v1/pixel.gif")
    @Operation(summary = "像素上报：返回1x1 GIF 并记录事件（支持批量）")
    public ResponseEntity<byte[]> pixel(
            @Parameter(description = "应用头（可选）") 
            @RequestHeader(value = HttpHeaderConstants.HEADER_APP_ID, required = false) Integer appIdHeader,
            
            // 应用ID参数（支持完整名和缩写）
            @Parameter(description = "应用ID URL参数（完整名，优先）") 
            @RequestParam(name = "appId", required = false) Integer appId,
            @RequestParam(name = "t", required = false) Integer appIdShort,
            
            // 单个事件参数（向后兼容，支持完整名和缩写）
            @Parameter(description = "事件名（单个模式，完整名）") 
            @RequestParam(name = "eventName", required = false, defaultValue = "pixel") String eventName,
            @RequestParam(name = "n", required = false) String eventNameShort,
            @Parameter(description = "事件内容 JSON（单个模式，完整名）") 
            @RequestParam(name = "eventContent", required = false) String eventContent,
            @RequestParam(name = "c", required = false) String eventContentShort,
            
            // 批量事件参数（支持完整名和缩写）
            @Parameter(description = "批量事件 Base64（批量模式，完整名）") 
            @RequestParam(name = "eventsB64", required = false) String eventsB64,
            @RequestParam(name = "b", required = false) String eventsB64Short,
            @Parameter(description = "批量模式标识（1=批量，0=单个，完整名）") 
            @RequestParam(name = "batch", required = false, defaultValue = "0") String batch,
            @RequestParam(name = "bt", required = false) String batchShort,
            
            // 会话和用户参数（支持完整名和缩写）
            @Parameter(description = "会话ID（完整名）") 
            @RequestParam(name = "sessionId", required = false) String sessionId,
            @RequestParam(name = "s", required = false) String sessionIdShort,
            @Parameter(description = "用户ID（完整名）") 
            @RequestParam(name = "userId", required = false) Integer userId,
            @RequestParam(name = "u", required = false) Integer userIdShort) {

        // 统一参数处理（优先使用缩写，回退到完整名）
        Integer finalTenantId = appIdShort != null ? appIdShort 
                              : (appId != null ? appId : appIdHeader);
        String finalSessionId = sessionIdShort != null ? sessionIdShort : sessionId;
        Integer finalUserId = userIdShort != null ? userIdShort : userId;
        String finalEventsB64 = eventsB64Short != null ? eventsB64Short : eventsB64;
        String finalBatch = batchShort != null ? batchShort : batch;
        String finalEventName = eventNameShort != null ? eventNameShort : eventName;
        String finalEventContent = eventContentShort != null ? eventContentShort : eventContent;
        
        if (finalTenantId == null) {
            log.warn("像素上报缺少 appId 参数");
            // 返回 GIF 但不记录事件
            return createGifResponse();
        }
    
        // 判断是否为批量模式
        boolean isBatch = "1".equals(finalBatch) || "true".equalsIgnoreCase(finalBatch);
        
        if (isBatch && finalEventsB64 != null && !finalEventsB64.isEmpty()) {
            // 批量模式：解析并保存多个事件
            return handleBatchPixel(finalTenantId, finalSessionId, finalUserId, finalEventsB64);
        } else {
            // 单个事件模式（向后兼容）
            return handleSinglePixel(finalTenantId, finalSessionId, finalUserId, finalEventName, finalEventContent);
        }
    }

    /**
     * 处理批量像素上报
     * 
     * @param appId 应用ID
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param eventsB64 Base64编码的批量事件JSON数组
     * @return GIF响应
     */
    private ResponseEntity<byte[]> handleBatchPixel(Integer appId, String sessionId, 
                                                     Integer userId, String eventsB64) {
        try {
            // URL-safe Base64 解码（处理 + 和 / 被替换为 - 和 _ 的情况）
            String normalizedB64 = eventsB64.replace('-', '+').replace('_', '/');
            // 补全填充
            int padding = normalizedB64.length() % 4;
            if (padding > 0) {
                normalizedB64 += "==".substring(0, 4 - padding);
            }
            
            // Base64 解码
            String decoded = new String(
                Base64.getDecoder().decode(normalizedB64), 
                StandardCharsets.UTF_8
            );
            
            // 解析JSON数组
            com.fasterxml.jackson.databind.ObjectMapper om = 
                new com.fasterxml.jackson.databind.ObjectMapper();
            List<PixelBatchEvent> events = om.readValue(
                decoded, 
                om.getTypeFactory().constructCollectionType(
                    List.class, 
                    PixelBatchEvent.class
                )
            );
            
            if (events == null || events.isEmpty()) {
                log.warn("批量像素上报：事件列表为空");
                return createGifResponse();
            }
            
            // 批量保存事件
            int saved = 0;
            for (PixelBatchEvent event : events) {
                try {
                    EventCollectRequest req = new EventCollectRequest();
                    req.setAppId(appId);
                    req.setSessionId(sessionId);
                    req.setUserId(userId);
                    
                    // 将压缩码转换为完整事件名
                    EventTypeEnum typeEnum = EventTypeEnum.fromCode(event.getType());
                    req.setEventName(typeEnum.getEventName());
                    req.setProperties(
                        event.getContent() != null && !event.getContent().isNull() 
                            ? event.getContent() 
                            : om.createObjectNode()
                    );
                    
                    eventService.save(req);
                    saved++;
                } catch (Exception e) {
                    log.warn("批量像素上报：保存单个事件失败: type={}", event.getType(), e);
                }
            }
            
            log.debug("批量像素上报成功: appId={}, count={}, saved={}", appId, events.size(), saved);
            
        } catch (IllegalArgumentException e) {
            log.error("批量像素上报：Base64解码失败: appId={}", appId, e);
        } catch (Exception e) {
            log.error("批量像素上报失败: appId={}", appId, e);
        }
        
        return createGifResponse();
    }

    /**
     * 处理单个像素上报（向后兼容）
     * 
     * @param appId 应用ID
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param eventName 事件名称
     * @param eventContent 事件内容JSON字符串
     * @return GIF响应
     */
    private ResponseEntity<byte[]> handleSinglePixel(Integer appId, String sessionId, 
                                                      Integer userId, String eventName, 
                                                      String eventContent) {
        log.debug("收到像素上报请求: appId={}, eventName={}, sessionId={}, hasEventContent={}", 
                appId, eventName, sessionId, eventContent != null);

        EventCollectRequest req = new EventCollectRequest();
        req.setAppId(appId);
        req.setEventName(eventName != null ? eventName : "pixel");
        req.setSessionId(sessionId);
        req.setUserId(userId);
        
        // 解析事件内容（如果提供）
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        if (eventContent != null && !eventContent.isEmpty()) {
            try {
                req.setProperties(om.readValue(eventContent, com.fasterxml.jackson.databind.node.ObjectNode.class));
            } catch (Exception e) {
                log.warn("解析 eventContent 失败: {}", eventContent, e);
                req.setProperties(om.createObjectNode());
            }
        } else {
            req.setProperties(om.createObjectNode());
        }
        
        // 持久化事件（忽略返回值）
        eventService.save(req);

        return createGifResponse();
    }

    /**
     * 创建GIF响应
     * 
     * @return GIF响应实体
     */
    private ResponseEntity<byte[]> createGifResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, "image/gif");
        headers.setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic().getHeaderValue());
        return new ResponseEntity<>(ONE_BY_ONE_GIF, headers, HttpStatus.OK);
    }
}

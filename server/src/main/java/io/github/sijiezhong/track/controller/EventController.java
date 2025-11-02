package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.constants.ApiConstants;
import io.github.sijiezhong.track.constants.HttpHeaderConstants;
import io.github.sijiezhong.track.dto.ApiResponse;
import io.github.sijiezhong.track.dto.BatchCollectResultItem;
import io.github.sijiezhong.track.dto.EventCollectRequest;
import io.github.sijiezhong.track.dto.IdempotentSummary;
import io.github.sijiezhong.track.exception.ErrorCode;
import io.github.sijiezhong.track.exception.ForbiddenException;
import io.github.sijiezhong.track.exception.ValidationException;
import io.github.sijiezhong.track.service.EventService;
import io.github.sijiezhong.track.service.IdempotencyService;
import io.github.sijiezhong.track.util.ResponseUtil;
import io.github.sijiezhong.track.util.UserAgentParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 事件上报控制器
 * 
 * <p>
 * 提供事件数据采集上报的RESTful接口，支持单条和批量上报。
 * 支持幂等性、多租户隔离、自动字段补齐等功能。
 * 
 * @author sijie
 */
@RestController
@RequestMapping(ApiConstants.API_PREFIX + "/events")
@Tag(name = "Event Ingest", description = "事件采集上报接口")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    private final EventService eventService;
    private final IdempotencyService idempotencyService; // 可能为 null（测试场景）
    private final Validator validator;

    public EventController(EventService eventService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) IdempotencyService idempotencyService,
            Validator validator) {
        this.eventService = eventService;
        this.idempotencyService = idempotencyService;
        this.validator = validator;
    }

    /**
     * 单条事件上报（POST）
     * 
     * @param idemKey        幂等键（可选）
     * @param headerTenantId 租户ID请求头（可选）
     * @param req            事件上报请求
     * @param httpRequest    HTTP请求对象
     * @return 事件创建结果
     */
    @Operation(summary = "单条事件上报", description = "提交单条事件；使用 Idempotency-Key 支持幂等", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Idempotent Replayed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request", content = @Content)
    })
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<IdempotentSummary>> collect(
            @Parameter(description = "幂等键，可选") @RequestHeader(value = HttpHeaderConstants.HEADER_IDEMPOTENCY_KEY, required = false) String idemKey,
            @RequestHeader(value = HttpHeaderConstants.HEADER_TENANT_ID, required = false) Integer headerTenantId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = EventCollectRequest.class))) @Valid @RequestBody EventCollectRequest req,
            HttpServletRequest httpRequest) {

        log.debug("收到单条事件上报请求: eventName={}, sessionId={}", req.getEventName(), req.getSessionId());

        // 幂等性检查
        if (idemKey != null && !idemKey.isEmpty()) {
            if (idempotencyService != null) {
                var found = idempotencyService.findSummary(idemKey);
                if (found.isPresent()) {
                    log.debug("幂等键已存在，返回已有结果: idemKey={}", idemKey);
                    return ResponseEntity.ok(ResponseUtil.success("幂等请求", found.get()));
                }
            }
        }

        // 多租户校验与补齐
        validateAndSetTenantId(req, headerTenantId);

        // 标准化与自动补齐：将 UA/Referer/IP 写入 event_content
        enrichRequestWithHeaders(req, httpRequest);

        var evt = eventService.save(req);
        IdempotentSummary summary = new IdempotentSummary(evt.getId(), evt.getEventName(), evt.getEventTime());

        log.debug("事件保存成功: eventId={}, eventName={}", evt.getId(), evt.getEventName());

        // 保存幂等键
        if (idemKey != null && !idemKey.isEmpty() && idempotencyService != null) {
            idempotencyService.checkAndSet(idemKey, summary);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseUtil.success(summary));
    }

    /**
     * 单条事件上报（GET）
     * 
     * @param idemKey        幂等键（可选）
     * @param headerTenantId 租户ID请求头（可选）
     * @param eventName      事件名称
     * @param sessionId      会话ID
     * @param userId         用户ID（可选）
     * @param tenantId       租户ID（可选）
     * @return 事件创建结果
     */
    @Operation(summary = "单条事件上报（GET）", description = "通过查询参数提交单条事件；支持 Idempotency-Key 幂等")
    @GetMapping("/collect")
    public ResponseEntity<ApiResponse<IdempotentSummary>> collectByGet(
            @Parameter(description = "幂等键，可选") @RequestHeader(value = HttpHeaderConstants.HEADER_IDEMPOTENCY_KEY, required = false) String idemKey,
            @RequestHeader(value = HttpHeaderConstants.HEADER_TENANT_ID, required = false) Integer headerTenantId,
            @Parameter(description = "事件名", required = true) @RequestParam(name = "eventName") String eventName,
            @Parameter(description = "会话ID", required = true) @RequestParam(name = "sessionId") String sessionId,
            @Parameter(description = "用户ID，可选") @RequestParam(name = "userId", required = false) Integer userId,
            @Parameter(description = "租户ID，可选") @RequestParam(name = "tenantId", required = false) Integer tenantId) {

        log.debug("收到GET方式事件上报请求: eventName={}, sessionId={}", eventName, sessionId);

        // 幂等性检查
        if (idemKey != null && !idemKey.isEmpty() && idempotencyService != null) {
            var found = idempotencyService.findSummary(idemKey);
            if (found.isPresent()) {
                log.debug("幂等键已存在，返回已有结果: idemKey={}", idemKey);
                return ResponseEntity.ok(ResponseUtil.success("幂等请求", found.get()));
            }
        }

        // 组装最小请求体并保存
        EventCollectRequest req = new EventCollectRequest();
        req.setEventName(eventName);
        req.setSessionId(sessionId);
        req.setUserId(userId);

        // 多租户校验与补齐
        if (headerTenantId != null) {
            if (tenantId != null && !tenantId.equals(headerTenantId)) {
                throw new ForbiddenException(ErrorCode.TENANT_ID_MISMATCH);
            }
            req.setTenantId(tenantId == null ? headerTenantId : tenantId);
        } else {
            req.setTenantId(tenantId);
        }

        var evt = eventService.save(req);
        IdempotentSummary summary = new IdempotentSummary(evt.getId(), evt.getEventName(), evt.getEventTime());

        log.debug("事件保存成功: eventId={}, eventName={}", evt.getId(), evt.getEventName());

        // 保存幂等键
        if (idemKey != null && !idemKey.isEmpty() && idempotencyService != null) {
            boolean firstHandled = idempotencyService.checkAndSet(idemKey, summary);
            if (!firstHandled) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseUtil.success(summary));
    }

    /**
     * 校验并设置租户ID
     * 
     * @param req            事件请求
     * @param headerTenantId 请求头中的租户ID
     */
    private void validateAndSetTenantId(EventCollectRequest req, Integer headerTenantId) {
        if (headerTenantId != null) {
            if (req.getTenantId() != null && !req.getTenantId().equals(headerTenantId)) {
                throw new ForbiddenException(ErrorCode.TENANT_ID_MISMATCH);
            }
            if (req.getTenantId() == null) {
                req.setTenantId(headerTenantId);
            }
        }
    }

    /**
     * 从HTTP请求中提取并补齐请求对象的字段
     * 
     * @param req         事件请求对象
     * @param httpRequest HTTP请求对象
     */
    private void enrichRequestWithHeaders(EventCollectRequest req, HttpServletRequest httpRequest) {
        if (httpRequest == null) {
            return;
        }

        String ua = httpRequest.getHeader(HttpHeaderConstants.HEADER_USER_AGENT);
        String referer = httpRequest.getHeader(HttpHeaderConstants.HEADER_REFERER);
        String xff = httpRequest.getHeader(HttpHeaderConstants.HEADER_X_FORWARDED_FOR);

        // 提取真实IP
        String ip = null;
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            ip = comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        if (ip == null || ip.isBlank()) {
            ip = httpRequest.getRemoteAddr();
        }

        // 将结构化字段填充到请求对象
        req.setUa(ua);
        req.setReferrer(referer);
        req.setIp(ip);

        var parsed = UserAgentParser.parse(ua);
        req.setDevice(parsed.device);
        req.setOs(parsed.os);
        req.setBrowser(parsed.browser);

        // 兼容：同时写回 properties，便于前端回显与向后兼容
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode node;
        if (req.getProperties() != null && req.getProperties().isObject()) {
            node = (com.fasterxml.jackson.databind.node.ObjectNode) req.getProperties();
        } else {
            node = om.createObjectNode();
            if (req.getProperties() != null) {
                node.put("_raw", req.getProperties().toString());
            }
        }
        if (ua != null && !ua.isBlank()) {
            node.put("ua", ua);
        }
        if (referer != null && !referer.isBlank()) {
            node.put("referrer", referer);
        }
        if (ip != null && !ip.isBlank()) {
            node.put("ip", ip);
        }
        if (parsed.device != null) {
            node.put("device", parsed.device);
        }
        if (parsed.os != null) {
            node.put("os", parsed.os);
        }
        if (parsed.browser != null) {
            node.put("browser", parsed.browser);
        }
        req.setProperties(node);
    }

    /**
     * 批量事件上报（严格校验）
     * 
     * <p>
     * 所有请求必须合法，任一非法则整体返回400错误。
     * 
     * @param requests 事件请求列表
     * @return 创建结果
     */
    @Operation(summary = "批量事件上报（严格校验）", description = "全部合法返回201；任一非法整体400", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request", content = @Content)
    })
    @PostMapping("/collect/batch")
    public ResponseEntity<ApiResponse<Void>> collectBatch(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = EventCollectRequest.class))) @Valid @RequestBody List<EventCollectRequest> requests) {

        log.info("收到批量事件上报请求: count={}", requests.size());

        // 验证每个请求
        for (EventCollectRequest req : requests) {
            var violations = validator.validate(req);
            if (!violations.isEmpty()) {
                List<String> details = violations.stream()
                        .map(ConstraintViolation::getMessage)
                        .toList();
                throw new ValidationException(ErrorCode.VALIDATION_ERROR, details);
            }
        }

        // 批量保存
        for (EventCollectRequest req : requests) {
            eventService.save(req);
        }

        log.info("批量事件保存成功: count={}", requests.size());

        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseUtil.success());
    }

    /**
     * 批量事件上报（部分失败策略）
     * 
     * <p>
     * 逐条校验并处理，返回每条的创建/失败状态。
     * 
     * @param requests 事件请求列表
     * @return 批量处理结果明细
     */
    @Operation(summary = "批量事件上报（部分失败策略）", description = "逐条校验并处理，返回每条的创建/失败状态", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = BatchCollectResultItem.class)))
    })
    @PostMapping("/collect/batch/result")
    public ResponseEntity<ApiResponse<List<BatchCollectResultItem>>> collectBatchWithResult(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(schema = @Schema(implementation = EventCollectRequest.class))) @RequestBody List<EventCollectRequest> requests) {

        log.info("收到批量事件上报请求（部分失败策略）: count={}", requests.size());

        List<BatchCollectResultItem> results = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            EventCollectRequest req = requests.get(i);
            var violations = validator.validate(req);
            if (!violations.isEmpty()) {
                String msg = violations.stream().findFirst().map(ConstraintViolation::getMessage).orElse("invalid");
                results.add(new BatchCollectResultItem(i, "failed", msg));
                continue;
            }
            eventService.save(req);
            results.add(new BatchCollectResultItem(i, "created", null));
        }

        log.info("批量事件处理完成: total={}, success={}, failed={}",
                requests.size(),
                results.stream().filter(r -> "created".equals(r.getStatus())).count(),
                results.stream().filter(r -> "failed".equals(r.getStatus())).count());

        return ResponseEntity.ok(ResponseUtil.success(results));
    }
}

package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.constants.ApiConstants;
import io.github.sijiezhong.track.constants.HttpHeaderConstants;
import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.dto.ApiResponse;
import io.github.sijiezhong.track.dto.EventListItem;
import io.github.sijiezhong.track.dto.PageResult;
import io.github.sijiezhong.track.exception.ErrorCode;
import io.github.sijiezhong.track.exception.ForbiddenException;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 事件查询控制器
 * 
 * <p>提供事件数据的查询接口，支持多条件过滤和分页查询。
 * 所有查询都会进行应用隔离，确保数据安全。
 * 
 * @author sijie
 */
@RestController
@RequestMapping(ApiConstants.API_PREFIX + "/events")
@Tag(name = "Event Query", description = "事件查询接口")
public class EventQueryController {

    private static final Logger log = LoggerFactory.getLogger(EventQueryController.class);

    private final EventRepository eventRepository;

    public EventQueryController(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * 分页查询事件
     * 
     * @param headerAppId 应用ID请求头（必填）
     * @param eventName 事件名称（可选）
     * @param appId 应用ID（可选）
     * @param sessionId 会话主键ID（可选）
     * @param startTime 起始时间（可选）
     * @param endTime 结束时间（可选）
     * @param page 页码（从0开始，默认0）
     * @param size 每页大小（默认20）
     * @return 分页查询结果
     */
    @GetMapping
    @Operation(summary = "分页查询事件", description = "支持按事件名/应用/会话/时间范围过滤")
    public ApiResponse<PageResult<EventListItem>> page(
            @Parameter(description = "应用头，必填") @RequestHeader(value = HttpHeaderConstants.HEADER_APP_ID, required = true) Integer headerAppId,
            @Parameter(description = "事件名") @RequestParam(name = "eventName", required = false) String eventName,
            @Parameter(description = "应用ID") @RequestParam(name = "appId", required = false) Integer appId,
            @Parameter(description = "会话主键ID") @RequestParam(name = "sessionId", required = false) Long sessionId,
            @Parameter(description = "起始时间") @RequestParam(name = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "页码(从0开始)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(name = "size", defaultValue = "20") int size) {
        
        log.debug("收到事件查询请求: appId={}, eventName={}, page={}, size={}", 
            headerAppId, eventName, page, size);
        
        // 应用ID校验
        if (headerAppId == null) {
            throw new ForbiddenException(ErrorCode.APP_ID_REQUIRED);
        }
        if (appId != null && !appId.equals(headerAppId)) {
            throw new ForbiddenException(ErrorCode.APP_ID_MISMATCH);
        }
        
        // 分页参数校验
        if (page < ApiConstants.DEFAULT_PAGE) {
            page = ApiConstants.DEFAULT_PAGE;
        }
        if (size < ApiConstants.MIN_PAGE_SIZE || size > ApiConstants.MAX_PAGE_SIZE) {
            size = ApiConstants.DEFAULT_PAGE_SIZE;
        }
        
        // 创建分页对象，按事件时间倒序（最新的在前）
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "eventTime"));
        
        // 构建查询条件
        Specification<Event> spec = Specification.where(null);
        
        // 应用ID过滤必须在最前面，这是关键的安全过滤
        spec = spec.and((root, query, cb) -> cb.equal(root.get("appId"), headerAppId));
        
        // 事件名过滤
        if (eventName != null && !eventName.isBlank()) {
            final String finalEventName = eventName; // 必须final才能用于lambda
            spec = spec.and((root, query, cb) -> cb.equal(root.get("eventName"), finalEventName));
        }
        
        // 会话ID过滤
        if (sessionId != null) {
            final Long finalSessionId = sessionId;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sessionId"), finalSessionId));
        }
        
        // 时间范围过滤
        if (startTime != null) {
            final LocalDateTime finalStartTime = startTime;
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventTime"), finalStartTime));
        }
        if (endTime != null) {
            final LocalDateTime finalEndTime = endTime;
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("eventTime"), finalEndTime));
        }
        
        Page<Event> pageResult = eventRepository.findAll(spec, pageable);
        List<EventListItem> content = pageResult.getContent().stream()
            .map(e -> new EventListItem(
                e.getId(),
                e.getEventName(),
                e.getUserId(),
                e.getSessionId(),
                e.getAppId(),
                e.getEventTime(),
                e.getProperties()
            ))
            .collect(Collectors.toList());
        
        PageResult<EventListItem> result = new PageResult<>(
            pageResult.getTotalElements(), 
            pageResult.getNumber(), 
            pageResult.getSize(), 
            content
        );
        
        log.debug("事件查询完成: total={}, page={}, size={}", result.getTotal(), page, size);
        
        return ResponseUtil.success(result);
    }
}

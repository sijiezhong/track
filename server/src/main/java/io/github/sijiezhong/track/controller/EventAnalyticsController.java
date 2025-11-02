package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.constants.ApiConstants;
import io.github.sijiezhong.track.constants.HttpHeaderConstants;
import io.github.sijiezhong.track.dto.ApiResponse;
import io.github.sijiezhong.track.exception.BusinessException;
import io.github.sijiezhong.track.exception.ErrorCode;
import io.github.sijiezhong.track.service.AnalyticsService;
import io.github.sijiezhong.track.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 事件分析控制器
 * 
 * <p>提供事件数据的统计分析功能，包括趋势、路径、留存、漏斗、分群和热点分析。
 * 所有分析接口都需要租户ID请求头进行数据隔离。
 * 
 * @author sijie
 */
@RestController
@RequestMapping(ApiConstants.API_PREFIX + "/events")
@Tag(name = "Event Analytics", description = "事件分析接口")
public class EventAnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(EventAnalyticsController.class);

    private final AnalyticsService analyticsService;

    public EventAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * 趋势分析（按天）
     * 
     * @param tenantId 租户ID请求头（必填）
     * @param eventName 事件名称
     * @param interval 时间粒度（仅支持daily）
     * @param startTime 起始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 趋势分析结果
     */
    @GetMapping("/trend")
    @Operation(summary = "趋势分析-按天", description = "interval=daily，返回 {date,count}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ApiResponse<List<Map<String, Object>>> trend(
            @Parameter(description = "租户头，必填") @RequestHeader(HttpHeaderConstants.HEADER_TENANT_ID) Integer tenantId,
            @Parameter(description = "事件名", required = true) @RequestParam("eventName") String eventName,
            @Parameter(description = "粒度，仅支持daily") @RequestParam(name = "interval", defaultValue = "daily") String interval,
            @Parameter(description = "起始时间") @RequestParam(name = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        log.debug("收到趋势分析请求: tenantId={}, eventName={}, interval={}", tenantId, eventName, interval);
        
        if (!"daily".equalsIgnoreCase(interval)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的粒度: " + interval);
        }
        
        List<Map<String, Object>> result = analyticsService.trendDaily(tenantId, eventName, startTime, endTime);
        return ResponseUtil.success(result);
    }

    /**
     * 路径分析（会话内二元序列）
     * 
     * @param tenantId 租户ID请求头（必填）
     * @param startTime 起始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 路径分析结果
     */
    @GetMapping("/path")
    @Operation(summary = "路径分析-会话内二元序列", description = "返回 {from,to,count} 列表")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ApiResponse<List<Map<String, Object>>> path(
            @Parameter(description = "租户头，必填") @RequestHeader(HttpHeaderConstants.HEADER_TENANT_ID) Integer tenantId,
            @Parameter(description = "起始时间") @RequestParam(name = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        log.debug("收到路径分析请求: tenantId={}", tenantId);
        
        List<Map<String, Object>> result = analyticsService.pathEdges(tenantId, startTime, endTime);
        return ResponseUtil.success(result);
    }

    /**
     * 留存分析（按天）
     * 
     * @param tenantId 租户ID请求头（必填）
     * @param cohortEvent cohort事件名称
     * @param returnEvent 回访事件名称
     * @param day 留存天数（如1表示第二天）
     * @param startTime 起始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 留存分析结果
     */
    @GetMapping("/retention")
    @Operation(summary = "留存分析-按天", description = "返回 {cohortDate, cohort, retained, rate}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ApiResponse<List<Map<String, Object>>> retention(
            @Parameter(description = "租户头，必填") @RequestHeader(HttpHeaderConstants.HEADER_TENANT_ID) Integer tenantId,
            @Parameter(description = "cohort事件名", required = true) @RequestParam("cohortEvent") String cohortEvent,
            @Parameter(description = "回访事件名", required = true) @RequestParam("returnEvent") String returnEvent,
            @Parameter(description = "留存天数，如1表示第二天") @RequestParam(name = "day", defaultValue = "1") Integer day,
            @Parameter(description = "起始时间") @RequestParam(name = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        log.debug("收到留存分析请求: tenantId={}, cohortEvent={}, returnEvent={}, day={}", 
            tenantId, cohortEvent, returnEvent, day);
        
        if (day == null || day < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "留存天数必须大于等于1");
        }
        
        List<Map<String, Object>> result = analyticsService.retentionDaily(tenantId, cohortEvent, returnEvent, day, startTime, endTime);
        return ResponseUtil.success(result);
    }

    /**
     * 漏斗分析
     * 
     * @param tenantId 租户ID请求头（必填）
     * @param stepsCsv 事件步骤（逗号分隔，如 a,b,c）
     * @param startTime 起始时间（可选）
     * @param endTime 结束时间（可选）
     * @param windowDays 窗口天数（可选）
     * @return 漏斗分析结果
     */
    @GetMapping("/funnel")
    @Operation(summary = "漏斗分析", description = "steps=以逗号分隔的事件序列，返回 {steps, counts, conversionRates}")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ApiResponse<Map<String, Object>> funnel(
            @Parameter(description = "租户头，必填") @RequestHeader(HttpHeaderConstants.HEADER_TENANT_ID) Integer tenantId,
            @Parameter(description = "事件步骤，逗号分隔，如 a,b,c") @RequestParam("steps") String stepsCsv,
            @Parameter(description = "起始时间") @RequestParam(name = "startTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(name = "endTime", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "窗口天数，可选") @RequestParam(name = "windowDays", required = false) Integer windowDays) {
        
        log.debug("收到漏斗分析请求: tenantId={}, steps={}", tenantId, stepsCsv);
        
        List<String> steps = Arrays.stream(stepsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        
        Map<String, Object> result = analyticsService.funnel(tenantId, steps, startTime, endTime);
        return ResponseUtil.success(result);
    }

    /**
     * 分群统计
     * 
     * @param tenantId 租户ID请求头（必填）
     * @param eventName 事件名称
     * @param by 分组字段（支持 browser|device|os|referrer）
     * @return 分群统计结果
     */
    @GetMapping("/segmentation")
    @Operation(summary = "分群统计", description = "按字段分组统计事件数量，by 支持 browser|device|os|referrer")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ApiResponse<Map<String, Object>> segmentation(
            @Parameter(description = "租户头，必填") @RequestHeader(HttpHeaderConstants.HEADER_TENANT_ID) Integer tenantId,
            @Parameter(description = "事件名") @RequestParam("eventName") String eventName,
            @Parameter(description = "分组字段", example = "browser") @RequestParam("by") String by) {
        
        log.debug("收到分群统计请求: tenantId={}, eventName={}, by={}", tenantId, eventName, by);
        
        Map<String, Object> result = analyticsService.segmentation(tenantId, eventName, by);
        return ResponseUtil.success(result);
    }

    /**
     * 热点图分析
     * 
     * @param tenantId 租户ID请求头（必填）
     * @param eventName 事件名称
     * @param bucket 时间粒度（目前仅支持hour）
     * @return 热点图分析结果
     */
    @GetMapping("/heatmap")
    @Operation(summary = "热点图", description = "按小时聚合，返回小时桶计数")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    public ApiResponse<Map<String, Object>> heatmap(
            @Parameter(description = "租户头，必填") @RequestHeader(HttpHeaderConstants.HEADER_TENANT_ID) Integer tenantId,
            @Parameter(description = "事件名") @RequestParam("eventName") String eventName,
            @Parameter(description = "时间粒度", example = "hour") @RequestParam(name = "bucket", defaultValue = "hour") String bucket) {
        
        log.debug("收到热点图分析请求: tenantId={}, eventName={}, bucket={}", tenantId, eventName, bucket);
        
        Map<String, Object> result = analyticsService.heatmap(tenantId, eventName, bucket);
        return ResponseUtil.success(result);
    }
}

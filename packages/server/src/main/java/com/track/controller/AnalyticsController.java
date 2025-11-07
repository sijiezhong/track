package com.track.controller;

import com.track.dto.*;
import com.track.repository.ProjectRepository;
import com.track.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 数据分析 Controller
 * 提供 Dashboard 前端所需的所有数据分析接口
 */
@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "数据分析接口")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    private final ProjectRepository projectRepository;
    
    @Autowired
    public AnalyticsController(AnalyticsService analyticsService, ProjectRepository projectRepository) {
        this.analyticsService = analyticsService;
        this.projectRepository = projectRepository;
    }
    
    @GetMapping("/overview")
    @Operation(summary = "概览 KPI", description = "获取指定时间区间内的 PV、UV、跳出率与平均停留")
    public ResponseEntity<OverviewResponse> getOverview(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "UTC") String timezone) {
        
        // 验证时区有效性
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezone);
        }
        
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        startTime = startTime != null ? startTime : LocalDateTime.now().minusDays(7);
        endTime = endTime != null ? endTime : LocalDateTime.now();
        
        long pv = analyticsService.getPV(appId, startTime, endTime, null);
        long uv = analyticsService.getUV(appId, startTime, endTime, null);
        double bounceRate = analyticsService.getBounceRate(appId, startTime, endTime);
        double avgDuration = analyticsService.getAvgDuration(appId, startTime, endTime);
        
        OverviewResponse response = new OverviewResponse(pv, uv, bounceRate, avgDuration, timezone);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pv-uv/series")
    @Operation(summary = "PV/UV 趋势", description = "获取区间内按时间粒度聚合的 PV/UV 序列")
    public ResponseEntity<PvUvSeriesResponse> getPvUvSeries(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "hour") String interval,
            @RequestParam(defaultValue = "UTC") String timezone) {
        
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        
        ZoneId zoneId = ZoneId.of(timezone);
        List<PvUvSeriesResponse.TimeSeriesPoint> series = analyticsService.getPvUvSeries(appId, startTime, endTime,
                interval, zoneId);
        
        PvUvSeriesResponse response = new PvUvSeriesResponse(series, interval, timezone);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/pages/top")
    @Operation(summary = "页面 TopN", description = "获取区间内页面指标 TopN")
    public ResponseEntity<PagesTopResponse> getPagesTop(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "UTC") String timezone) {
        
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        
        List<PagesTopResponse.PageStats> list = analyticsService.getPagesTop(appId, startTime, endTime, limit);
        long total = analyticsService.getPagesTopTotal(appId, startTime, endTime);
        
        PagesTopResponse response = new PagesTopResponse(list, total);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/events-distribution")
    @Operation(summary = "事件类型分布", description = "区间内事件类型占比分布")
    public ResponseEntity<EventsDistributionResponse> getEventsDistribution(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        
        List<EventsDistributionResponse.TypeDistribution> list = analyticsService.getEventsDistribution(appId,
                startTime, endTime);
        
        EventsDistributionResponse response = new EventsDistributionResponse(list);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/web-vitals")
    @Operation(summary = "Web Vitals 分位", description = "区间内 Web Vitals 指标的分位统计")
    public ResponseEntity<WebVitalsResponse> getWebVitals(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "LCP") String metric,
            @RequestParam(defaultValue = "UTC") String timezone) {
        
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        
        WebVitalsResponse response = analyticsService.getWebVitals(appId, startTime, endTime, metric);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/web-vitals/series")
    @Operation(summary = "Web Vitals 趋势", description = "区间内 Web Vitals 指标趋势")
    public ResponseEntity<WebVitalsSeriesResponse> getWebVitalsSeries(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam String metric,
            @RequestParam(defaultValue = "hour") String interval,
            @RequestParam(defaultValue = "UTC") String timezone) {
        
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        
        ZoneId zoneId = ZoneId.of(timezone);
        List<WebVitalsSeriesResponse.WebVitalsPoint> series = analyticsService.getWebVitalsSeries(appId, startTime,
                endTime, metric, interval, zoneId);
        
        WebVitalsSeriesResponse response = new WebVitalsSeriesResponse(series, interval, timezone);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/custom-events")
    @Operation(summary = "自定义事件趋势", description = "按事件 ID 聚合的趋势数据")
    public ResponseEntity<CustomEventsSeriesResponse> getCustomEvents(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestParam(defaultValue = "UTC") String timezone) {
        
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        
        ZoneId zoneId = ZoneId.of(timezone);
        List<CustomEventsSeriesResponse.CustomEventPoint> series = analyticsService.getCustomEventsSeries(appId,
                eventId, startTime, endTime, groupBy, zoneId);
        long total = analyticsService.getCustomEventsTotal(appId, eventId, startTime, endTime);
        
        CustomEventsSeriesResponse response = new CustomEventsSeriesResponse(series, total, groupBy);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/custom-events/top")
    @Operation(summary = "自定义事件 TopN", description = "区间内自定义事件 TopN")
    public ResponseEntity<CustomEventsTopResponse> getCustomEventsTop(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "10") int limit) {
        
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        
        List<CustomEventsTopResponse.CustomEventStats> list = analyticsService.getCustomEventsTop(appId, startTime,
                endTime, limit);
        long total = analyticsService.getCustomEventsTotal(appId, null, startTime, endTime);
        
        CustomEventsTopResponse response = new CustomEventsTopResponse(list, total);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/errors/trend")
    @Operation(summary = "错误趋势", description = "区间内错误数量趋势")
    public ResponseEntity<ErrorsTrendResponse> getErrorsTrend(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "hour") String interval,
            @RequestParam(defaultValue = "UTC") String timezone) {
        
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        
        ZoneId zoneId = ZoneId.of(timezone);
        List<ErrorsTrendResponse.ErrorPoint> series = analyticsService.getErrorsTrend(appId, startTime, endTime,
                interval, zoneId);
        
        ErrorsTrendResponse response = new ErrorsTrendResponse(series, interval, timezone);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/errors/top")
    @Operation(summary = "错误 TopN", description = "区间内错误 TopN")
    public ResponseEntity<ErrorsTopResponse> getErrorsTop(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "10") int limit) {
        
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        
        List<ErrorsTopResponse.ErrorStats> list = analyticsService.getErrorsTop(appId, startTime, endTime, limit);
        
        // 计算错误总数（使用简单计数，避免重复查询）
        long total = list.stream().mapToLong(ErrorsTopResponse.ErrorStats::getCount).sum();
        
        ErrorsTopResponse response = new ErrorsTopResponse(list, total);
        return ResponseEntity.ok(response);
    }

    /**
     * 解析日期时间字符串，处理空字符串和 null
     * 
     * @param dateTimeStr ISO 8601 格式的日期时间字符串
     * @return LocalDateTime 对象，如果输入为空或解析失败则返回 null
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            // 解析失败时返回 null，而不是抛出异常
            return null;
        }
    }
}

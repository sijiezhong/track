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

import java.time.LocalDate;
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
            @RequestParam(required = true) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "UTC") String timezone) {

        // 验证 appId
        validateAppId(appId);

        // 验证时区有效性
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezone);
        }

        LocalDateTime startTime = parseStartDateTime(start);
        LocalDateTime endTime = parseEndDateTime(end);
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
            @RequestParam(required = true) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "hour") String interval,
            @RequestParam(defaultValue = "UTC") String timezone) {

        // 验证 appId
        validateAppId(appId);

        LocalDateTime startTime = parseStartDateTime(start);
        LocalDateTime endTime = parseEndDateTime(end);

        ZoneId zoneId = ZoneId.of(timezone);
        List<PvUvSeriesResponse.TimeSeriesPoint> series = analyticsService.getPvUvSeries(appId, startTime, endTime,
                interval, zoneId);

        PvUvSeriesResponse response = new PvUvSeriesResponse(series, interval, timezone);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pages/top")
    @Operation(summary = "页面 TopN", description = "获取区间内页面指标 TopN")
    public ResponseEntity<PagesTopResponse> getPagesTop(
            @RequestParam(required = true) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "UTC") String timezone) {

        // 验证 appId
        validateAppId(appId);

        LocalDateTime startTime = parseStartDateTime(start);
        LocalDateTime endTime = parseEndDateTime(end);

        List<PagesTopResponse.PageStats> list = analyticsService.getPagesTop(appId, startTime, endTime, limit);
        long total = analyticsService.getPagesTopTotal(appId, startTime, endTime);

        PagesTopResponse response = new PagesTopResponse(list, total);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/events-distribution")
    @Operation(summary = "事件类型分布", description = "区间内事件类型占比分布")
    public ResponseEntity<EventsDistributionResponse> getEventsDistribution(
            @RequestParam(required = true) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {

        // 验证 appId
        validateAppId(appId);

        LocalDateTime startTime = parseStartDateTime(start);
        LocalDateTime endTime = parseEndDateTime(end);

        List<EventsDistributionResponse.TypeDistribution> list = analyticsService.getEventsDistribution(appId,
                startTime, endTime);

        EventsDistributionResponse response = new EventsDistributionResponse(list);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/web-vitals")
    @Operation(summary = "Web Vitals 分位", description = "区间内 Web Vitals 指标的分位统计")
    public ResponseEntity<WebVitalsResponse> getWebVitals(
            @RequestParam(required = true) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "LCP") String metric,
            @RequestParam(defaultValue = "UTC") String timezone) {

        // 验证 appId
        validateAppId(appId);

        LocalDateTime startTime = parseStartDateTime(start);
        LocalDateTime endTime = parseEndDateTime(end);

        WebVitalsResponse response = analyticsService.getWebVitals(appId, startTime, endTime, metric);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/web-vitals/series")
    @Operation(summary = "Web Vitals 趋势", description = "区间内 Web Vitals 指标趋势")
    public ResponseEntity<WebVitalsSeriesResponse> getWebVitalsSeries(
            @RequestParam(required = true) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam String metric,
            @RequestParam(defaultValue = "hour") String interval,
            @RequestParam(defaultValue = "UTC") String timezone) {

        // 验证 appId
        validateAppId(appId);

        LocalDateTime startTime = parseStartDateTime(start);
        LocalDateTime endTime = parseEndDateTime(end);

        ZoneId zoneId = ZoneId.of(timezone);
        List<WebVitalsSeriesResponse.WebVitalsPoint> series = analyticsService.getWebVitalsSeries(appId, startTime,
                endTime, metric, interval, zoneId);

        WebVitalsSeriesResponse response = new WebVitalsSeriesResponse(series, interval, timezone);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/custom-events")
    @Operation(summary = "自定义事件趋势", description = "按事件 ID 聚合的趋势数据")
    public ResponseEntity<CustomEventsSeriesResponse> getCustomEvents(
            @RequestParam(required = true) String appId,
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "day") String groupBy,
            @RequestParam(defaultValue = "UTC") String timezone) {

        // 验证 appId
        validateAppId(appId);

        LocalDateTime startTime = parseStartDateTime(start);
        LocalDateTime endTime = parseEndDateTime(end);

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
            @RequestParam(required = true) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "10") int limit) {

        // 验证 appId
        validateAppId(appId);

        LocalDateTime startTime = parseStartDateTime(start);
        LocalDateTime endTime = parseEndDateTime(end);

        List<CustomEventsTopResponse.CustomEventStats> list = analyticsService.getCustomEventsTop(appId, startTime,
                endTime, limit);
        long total = analyticsService.getCustomEventsTotal(appId, null, startTime, endTime);

        CustomEventsTopResponse response = new CustomEventsTopResponse(list, total);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/errors/trend")
    @Operation(summary = "错误趋势", description = "区间内错误数量趋势")
    public ResponseEntity<ErrorsTrendResponse> getErrorsTrend(
            @RequestParam(required = true) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "hour") String interval,
            @RequestParam(defaultValue = "UTC") String timezone) {

        // 验证 appId
        validateAppId(appId);

        LocalDateTime startTime = parseStartDateTime(start);
        LocalDateTime endTime = parseEndDateTime(end);

        ZoneId zoneId = ZoneId.of(timezone);
        List<ErrorsTrendResponse.ErrorPoint> series = analyticsService.getErrorsTrend(appId, startTime, endTime,
                interval, zoneId);

        ErrorsTrendResponse response = new ErrorsTrendResponse(series, interval, timezone);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/errors/top")
    @Operation(summary = "错误 TopN", description = "区间内错误 TopN")
    public ResponseEntity<ErrorsTopResponse> getErrorsTop(
            @RequestParam(required = true) String appId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(defaultValue = "10") int limit) {

        // 验证 appId
        validateAppId(appId);

        LocalDateTime startTime = parseStartDateTime(start);
        LocalDateTime endTime = parseEndDateTime(end);

        List<ErrorsTopResponse.ErrorStats> list = analyticsService.getErrorsTop(appId, startTime, endTime, limit);

        // 计算错误总数（使用简单计数，避免重复查询）
        long total = list.stream().mapToLong(ErrorsTopResponse.ErrorStats::getCount).sum();

        ErrorsTopResponse response = new ErrorsTopResponse(list, total);
        return ResponseEntity.ok(response);
    }

    /**
     * 解析开始时间字符串
     * 如果只传日期（yyyy-MM-dd），自动拼接为当天的 00:00:00
     * 
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime 对象，如果输入为空或解析失败则返回 null
     */
    private LocalDateTime parseStartDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            // 如果是纯日期格式（yyyy-MM-dd），自动拼接为当天的 00:00:00
            if (dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dateTimeStr).atStartOfDay();
            }
            // 尝试 ISO_DATE_TIME 格式（完整格式，包含秒和时区）
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e1) {
            try {
                // 尝试 ISO_LOCAL_DATE_TIME 格式（不包含时区）
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e2) {
                try {
                    // 尝试简化格式：yyyy-MM-ddTHH:mm（缺少秒数）
                    if (dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) {
                        return LocalDateTime.parse(dateTimeStr + ":00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    }
                } catch (Exception e3) {
                    // 所有解析都失败，返回 null
                }
                return null;
            }
        }
    }

    /**
     * 解析结束时间字符串
     * 如果只传日期（yyyy-MM-dd），自动拼接为当天的 23:59:59
     * 
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime 对象，如果输入为空或解析失败则返回 null
     */
    private LocalDateTime parseEndDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            // 如果是纯日期格式（yyyy-MM-dd），自动拼接为当天的 23:59:59
            if (dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(dateTimeStr).atTime(23, 59, 59);
            }
            // 尝试 ISO_DATE_TIME 格式（完整格式，包含秒和时区）
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e1) {
            try {
                // 尝试 ISO_LOCAL_DATE_TIME 格式（不包含时区）
                return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e2) {
                try {
                    // 尝试简化格式：yyyy-MM-ddTHH:mm（缺少秒数）
                    if (dateTimeStr.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) {
                        return LocalDateTime.parse(dateTimeStr + ":00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    }
                } catch (Exception e3) {
                    // 所有解析都失败，返回 null
                }
                return null;
            }
        }
    }

    /**
     * 验证 appId 参数
     * @param appId 应用 ID
     * @throws IllegalArgumentException 如果 appId 为空或空字符串
     */
    private void validateAppId(String appId) {
        if (appId == null || appId.trim().isEmpty()) {
            throw new IllegalArgumentException("appId is required and cannot be empty");
        }
    }
}

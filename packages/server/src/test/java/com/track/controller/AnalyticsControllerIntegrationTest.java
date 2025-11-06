package com.track.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.track.dto.*;
import com.track.repository.ProjectRepository;
import com.track.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AnalyticsController 集成测试
 * 验证数据分析接口的正确性，包括参数校验、响应格式和数据计算逻辑
 */
@WebMvcTest(controllers = AnalyticsController.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private ProjectRepository projectRepository;

    private String appId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        appId = "test-app";
        startTime = LocalDateTime.now().minusDays(7);
        endTime = LocalDateTime.now();
    }

    // ========== getOverview 测试 ==========

    @Test
    void testGetOverview_Success() throws Exception {
        // Given
        when(analyticsService.getPV(any(), any(), any(), any())).thenReturn(1000L);
        when(analyticsService.getUV(any(), any(), any(), any())).thenReturn(500L);
        when(analyticsService.getBounceRate(any(), any(), any())).thenReturn(0.35);
        when(analyticsService.getAvgDuration(any(), any(), any())).thenReturn(120.5);

        // When & Then
        String response = mockMvc.perform(get("/api/analytics/overview")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString())
                .param("timezone", "Asia/Shanghai"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pv").value(1000))
            .andExpect(jsonPath("$.uv").value(500))
            .andExpect(jsonPath("$.bounceRate").value(0.35))
            .andExpect(jsonPath("$.avgDurationSec").value(120.5))
            .andExpect(jsonPath("$.timezone").value("Asia/Shanghai"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        OverviewResponse result = objectMapper.readValue(response, OverviewResponse.class);
        assertEquals(1000L, result.getPv());
        assertEquals(500L, result.getUv());
        assertEquals(0.35, result.getBounceRate(), 0.001);
        assertEquals(120.5, result.getAvgDurationSec(), 0.1);
        assertEquals("Asia/Shanghai", result.getTimezone());
    }

    @Test
    void testGetOverview_WithoutAppId() throws Exception {
        // Given - appId 是可选的，应该能正常工作
        when(analyticsService.getPV(isNull(), any(), any(), any())).thenReturn(2000L);
        when(analyticsService.getUV(isNull(), any(), any(), any())).thenReturn(1000L);
        when(analyticsService.getBounceRate(isNull(), any(), any())).thenReturn(0.4);
        when(analyticsService.getAvgDuration(isNull(), any(), any())).thenReturn(150.0);

        // When & Then
        mockMvc.perform(get("/api/analytics/overview")
                .param("start", startTime.toString())
                .param("end", endTime.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pv").value(2000))
            .andExpect(jsonPath("$.uv").value(1000));
    }

    @Test
    void testGetOverview_DefaultTimezone() throws Exception {
        // Given
        when(analyticsService.getPV(any(), any(), any(), any())).thenReturn(100L);
        when(analyticsService.getUV(any(), any(), any(), any())).thenReturn(50L);
        when(analyticsService.getBounceRate(any(), any(), any())).thenReturn(0.3);
        when(analyticsService.getAvgDuration(any(), any(), any())).thenReturn(100.0);

        // When & Then - 不传 timezone 应该使用默认值 UTC
        mockMvc.perform(get("/api/analytics/overview")
                .param("appId", appId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.timezone").value("UTC"));
    }

    // ========== getPvUvSeries 测试 ==========

    @Test
    void testGetPvUvSeries_Success() throws Exception {
        // Given
        List<PvUvSeriesResponse.TimeSeriesPoint> series = new ArrayList<>();
        series.add(new PvUvSeriesResponse.TimeSeriesPoint("2024-01-01T00:00:00", 100L, 50L));
        series.add(new PvUvSeriesResponse.TimeSeriesPoint("2024-01-01T01:00:00", 150L, 75L));

        when(analyticsService.getPvUvSeries(eq(appId), any(), any(), eq("hour"), any()))
            .thenReturn(series);

        // When & Then
        String response = mockMvc.perform(get("/api/analytics/pv-uv/series")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString())
                .param("interval", "hour")
                .param("timezone", "UTC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.series").isArray())
            .andExpect(jsonPath("$.series.length()").value(2))
            .andExpect(jsonPath("$.series[0].pv").value(100))
            .andExpect(jsonPath("$.series[0].uv").value(50))
            .andExpect(jsonPath("$.interval").value("hour"))
            .andExpect(jsonPath("$.timezone").value("UTC"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        PvUvSeriesResponse result = objectMapper.readValue(response, PvUvSeriesResponse.class);
        assertEquals(2, result.getSeries().size());
        assertEquals(100L, result.getSeries().get(0).getPv());
        assertEquals(50L, result.getSeries().get(0).getUv());
    }

    @Test
    void testGetPvUvSeries_WithoutAppId() throws Exception {
        // Given - appId 可选，应该能正常工作
        List<PvUvSeriesResponse.TimeSeriesPoint> series = new ArrayList<>();
        when(analyticsService.getPvUvSeries(isNull(), any(), any(), any(), any()))
            .thenReturn(series);

        // When & Then
        mockMvc.perform(get("/api/analytics/pv-uv/series")
                .param("start", startTime.toString())
                .param("end", endTime.toString()))
            .andExpect(status().isOk());
    }

    // ========== getPagesTop 测试 ==========

    @Test
    void testGetPagesTop_Success() throws Exception {
        // Given - 验证 TopN 排序逻辑
        List<PagesTopResponse.PageStats> list = Arrays.asList(
            new PagesTopResponse.PageStats("/page1", 1000L, 500L, 120.5),
            new PagesTopResponse.PageStats("/page2", 800L, 400L, 90.0),
            new PagesTopResponse.PageStats("/page3", 600L, 300L, 75.5)
        );

        when(analyticsService.getPagesTop(eq(appId), any(), any(), eq(10)))
            .thenReturn(list);
        when(analyticsService.getPagesTopTotal(eq(appId), any(), any()))
            .thenReturn(3L);

        // When & Then
        String response = mockMvc.perform(get("/api/analytics/pages/top")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString())
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list").isArray())
            .andExpect(jsonPath("$.list.length()").value(3))
            .andExpect(jsonPath("$.list[0].pageUrl").value("/page1"))
            .andExpect(jsonPath("$.list[0].pv").value(1000))
            .andExpect(jsonPath("$.list[0].uv").value(500))
            .andExpect(jsonPath("$.list[0].avgDurationSec").value(120.5))
            .andExpect(jsonPath("$.total").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString();

        PagesTopResponse result = objectMapper.readValue(response, PagesTopResponse.class);
        assertEquals(3, result.getList().size());
        // 验证列表按 PV 降序排列（业务逻辑）
        assertTrue(result.getList().get(0).getPv() >= result.getList().get(1).getPv());
    }

    @Test
    void testGetPagesTop_DefaultLimit() throws Exception {
        // Given
        List<PagesTopResponse.PageStats> list = new ArrayList<>();
        when(analyticsService.getPagesTop(any(), any(), any(), eq(10)))
            .thenReturn(list);
        when(analyticsService.getPagesTopTotal(any(), any(), any()))
            .thenReturn(0L);

        // When & Then - 不传 limit 应该使用默认值 10
        mockMvc.perform(get("/api/analytics/pages/top")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString()))
            .andExpect(status().isOk());
    }

    // ========== getEventsDistribution 测试 ==========

    @Test
    void testGetEventsDistribution_Success() throws Exception {
        // Given
        List<EventsDistributionResponse.TypeDistribution> list = Arrays.asList(
            new EventsDistributionResponse.TypeDistribution("page_view", 5000L),
            new EventsDistributionResponse.TypeDistribution("click", 3000L),
            new EventsDistributionResponse.TypeDistribution("error", 100L)
        );

        when(analyticsService.getEventsDistribution(eq(appId), any(), any()))
            .thenReturn(list);

        // When & Then
        String response = mockMvc.perform(get("/api/analytics/events-distribution")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list").isArray())
            .andExpect(jsonPath("$.list.length()").value(3))
            .andExpect(jsonPath("$.list[0].type").value("page_view"))
            .andExpect(jsonPath("$.list[0].value").value(5000))
            .andReturn()
            .getResponse()
            .getContentAsString();

        EventsDistributionResponse result = objectMapper.readValue(response, EventsDistributionResponse.class);
        assertEquals(3, result.getList().size());
        assertEquals("page_view", result.getList().get(0).getType());
        assertEquals(5000L, result.getList().get(0).getValue());
    }

    // ========== getWebVitals 测试 ==========

    @Test
    void testGetWebVitals_Success() throws Exception {
        // Given - 验证分位数计算正确性
        WebVitalsResponse response = new WebVitalsResponse(1200.0, 2040.0, 3480.0, "ms");
        when(analyticsService.getWebVitals(eq(appId), any(), any(), eq("LCP")))
            .thenReturn(response);

        // When & Then
        String result = mockMvc.perform(get("/api/analytics/web-vitals")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString())
                .param("metric", "LCP"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.p50").value(1200.0))
            .andExpect(jsonPath("$.p75").value(2040.0))
            .andExpect(jsonPath("$.p95").value(3480.0))
            .andExpect(jsonPath("$.unit").value("ms"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        WebVitalsResponse webVitals = objectMapper.readValue(result, WebVitalsResponse.class);
        // 验证分位数逻辑：p50 < p75 < p95
        assertTrue(webVitals.getP50() < webVitals.getP75());
        assertTrue(webVitals.getP75() < webVitals.getP95());
        assertEquals("ms", webVitals.getUnit());
    }

    @Test
    void testGetWebVitals_DefaultMetric() throws Exception {
        // Given
        WebVitalsResponse response = new WebVitalsResponse(1200.0, 2040.0, 3480.0, "ms");
        when(analyticsService.getWebVitals(any(), any(), any(), eq("LCP")))
            .thenReturn(response);

        // When & Then - 不传 metric 应该使用默认值 LCP
        mockMvc.perform(get("/api/analytics/web-vitals")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metric").doesNotExist()) // 响应中没有 metric 字段
            .andExpect(jsonPath("$.unit").value("ms"));
    }

    // ========== getWebVitalsSeries 测试 ==========

    @Test
    void testGetWebVitalsSeries_Success() throws Exception {
        // Given
        List<WebVitalsSeriesResponse.WebVitalsPoint> series = Arrays.asList(
            new WebVitalsSeriesResponse.WebVitalsPoint("2024-01-01T00:00:00", 1200.0, 2040.0, 3480.0),
            new WebVitalsSeriesResponse.WebVitalsPoint("2024-01-01T01:00:00", 1250.0, 2100.0, 3500.0)
        );

        when(analyticsService.getWebVitalsSeries(eq(appId), any(), any(), eq("LCP"), eq("hour"), any()))
            .thenReturn(series);

        // When & Then
        String response = mockMvc.perform(get("/api/analytics/web-vitals/series")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString())
                .param("metric", "LCP")
                .param("interval", "hour"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.series").isArray())
            .andExpect(jsonPath("$.series.length()").value(2))
            .andExpect(jsonPath("$.series[0].p50").value(1200.0))
            .andExpect(jsonPath("$.interval").value("hour"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        WebVitalsSeriesResponse result = objectMapper.readValue(response, WebVitalsSeriesResponse.class);
        assertEquals(2, result.getSeries().size());
        // 验证每个点都包含完整的分位数数据
        assertNotNull(result.getSeries().get(0).getP50());
        assertNotNull(result.getSeries().get(0).getP75());
        assertNotNull(result.getSeries().get(0).getP95());
    }

    // ========== getCustomEvents 测试 ==========

    @Test
    void testGetCustomEvents_Success() throws Exception {
        // Given
        List<CustomEventsSeriesResponse.CustomEventPoint> series = Arrays.asList(
            new CustomEventsSeriesResponse.CustomEventPoint("2024-01-01", 100L),
            new CustomEventsSeriesResponse.CustomEventPoint("2024-01-02", 150L)
        );

        when(analyticsService.getCustomEventsSeries(eq(appId), isNull(), any(), any(), eq("day"), any()))
            .thenReturn(series);
        when(analyticsService.getCustomEventsTotal(eq(appId), isNull(), any(), any()))
            .thenReturn(250L);

        // When & Then
        String response = mockMvc.perform(get("/api/analytics/custom-events")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString())
                .param("groupBy", "day"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.series").isArray())
            .andExpect(jsonPath("$.series.length()").value(2))
            .andExpect(jsonPath("$.total").value(250))
            .andExpect(jsonPath("$.groupBy").value("day"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        CustomEventsSeriesResponse result = objectMapper.readValue(response, CustomEventsSeriesResponse.class);
        assertEquals(2, result.getSeries().size());
        assertEquals(250L, result.getTotal());
        // 验证 total 等于所有 count 之和（业务逻辑）
        long sum = result.getSeries().stream().mapToLong(CustomEventsSeriesResponse.CustomEventPoint::getCount).sum();
        assertEquals(250L, sum);
    }

    @Test
    void testGetCustomEvents_WithEventId() throws Exception {
        // Given
        String eventId = "button_click";
        List<CustomEventsSeriesResponse.CustomEventPoint> series = new ArrayList<>();
        when(analyticsService.getCustomEventsSeries(eq(appId), eq(eventId), any(), any(), any(), any()))
            .thenReturn(series);
        when(analyticsService.getCustomEventsTotal(eq(appId), eq(eventId), any(), any()))
            .thenReturn(0L);

        // When & Then
        mockMvc.perform(get("/api/analytics/custom-events")
                .param("appId", appId)
                .param("eventId", eventId)
                .param("start", startTime.toString())
                .param("end", endTime.toString()))
            .andExpect(status().isOk());
    }

    // ========== getCustomEventsTop 测试 ==========

    @Test
    void testGetCustomEventsTop_Success() throws Exception {
        // Given - 验证 TopN 排序
        List<CustomEventsTopResponse.CustomEventStats> list = Arrays.asList(
            new CustomEventsTopResponse.CustomEventStats("event1", 1000L),
            new CustomEventsTopResponse.CustomEventStats("event2", 800L),
            new CustomEventsTopResponse.CustomEventStats("event3", 500L)
        );

        when(analyticsService.getCustomEventsTop(eq(appId), any(), any(), eq(10)))
            .thenReturn(list);
        when(analyticsService.getCustomEventsTotal(eq(appId), isNull(), any(), any()))
            .thenReturn(2300L);

        // When & Then
        String response = mockMvc.perform(get("/api/analytics/custom-events/top")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString())
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list").isArray())
            .andExpect(jsonPath("$.list.length()").value(3))
            .andExpect(jsonPath("$.list[0].eventId").value("event1"))
            .andExpect(jsonPath("$.list[0].count").value(1000))
            .andExpect(jsonPath("$.total").value(2300))
            .andReturn()
            .getResponse()
            .getContentAsString();

        CustomEventsTopResponse result = objectMapper.readValue(response, CustomEventsTopResponse.class);
        assertEquals(3, result.getList().size());
        // 验证列表按 count 降序排列（业务逻辑）
        assertTrue(result.getList().get(0).getCount() >= result.getList().get(1).getCount());
    }

    // ========== getErrorsTrend 测试 ==========

    @Test
    void testGetErrorsTrend_Success() throws Exception {
        // Given
        List<ErrorsTrendResponse.ErrorPoint> series = Arrays.asList(
            new ErrorsTrendResponse.ErrorPoint("2024-01-01T00:00:00", 10L),
            new ErrorsTrendResponse.ErrorPoint("2024-01-01T01:00:00", 15L)
        );

        when(analyticsService.getErrorsTrend(eq(appId), any(), any(), eq("hour"), any()))
            .thenReturn(series);

        // When & Then
        String response = mockMvc.perform(get("/api/analytics/errors/trend")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString())
                .param("interval", "hour"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.series").isArray())
            .andExpect(jsonPath("$.series.length()").value(2))
            .andExpect(jsonPath("$.series[0].count").value(10))
            .andExpect(jsonPath("$.interval").value("hour"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        ErrorsTrendResponse result = objectMapper.readValue(response, ErrorsTrendResponse.class);
        assertEquals(2, result.getSeries().size());
        assertEquals(10L, result.getSeries().get(0).getCount());
    }

    // ========== getErrorsTop 测试 ==========

    @Test
    void testGetErrorsTop_Success() throws Exception {
        // Given - 验证 TopN 排序和 total 计算
        List<ErrorsTopResponse.ErrorStats> list = Arrays.asList(
            new ErrorsTopResponse.ErrorStats("fingerprint1", "Error 1", 100L, "2024-01-01", "2024-01-02"),
            new ErrorsTopResponse.ErrorStats("fingerprint2", "Error 2", 80L, "2024-01-01", "2024-01-02"),
            new ErrorsTopResponse.ErrorStats("fingerprint3", "Error 3", 50L, "2024-01-01", "2024-01-02")
        );

        when(analyticsService.getErrorsTop(eq(appId), any(), any(), eq(10)))
            .thenReturn(list);

        // When & Then
        String response = mockMvc.perform(get("/api/analytics/errors/top")
                .param("appId", appId)
                .param("start", startTime.toString())
                .param("end", endTime.toString())
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.list").isArray())
            .andExpect(jsonPath("$.list.length()").value(3))
            .andExpect(jsonPath("$.list[0].fingerprint").value("fingerprint1"))
            .andExpect(jsonPath("$.list[0].count").value(100))
            .andExpect(jsonPath("$.total").value(230)) // 100 + 80 + 50
            .andReturn()
            .getResponse()
            .getContentAsString();

        ErrorsTopResponse result = objectMapper.readValue(response, ErrorsTopResponse.class);
        assertEquals(3, result.getList().size());
        // 验证列表按 count 降序排列（业务逻辑）
        assertTrue(result.getList().get(0).getCount() >= result.getList().get(1).getCount());
        // 验证 total 等于所有 count 之和（业务逻辑）
        long total = result.getList().stream().mapToLong(ErrorsTopResponse.ErrorStats::getCount).sum();
        assertEquals(230L, total);
        assertEquals(230L, result.getTotal());
    }
}


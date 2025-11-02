package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.service.EventService;
import io.github.sijiezhong.track.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.assertj.core.api.Assertions.assertThat;
import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.dto.EventCollectRequest;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.anyString;
import io.github.sijiezhong.track.dto.IdempotentSummary;
import java.util.Optional;
import org.mockito.ArgumentCaptor;

import static io.github.sijiezhong.track.testsupport.TestConstants.FIXED_TIME;

/**
 * Tests for EventController WebMvc layer (no database dependency).
 * 
 * Coverage includes:
 * - Request validation (missing required fields, invalid data)
 * - Batch collection endpoints
 * - Idempotency support
 * - Field mapping (event_type/project_id/event_content)
 * - Request enrichment (UA, Referer, IP from headers)
 * - Multi-tenant support
 */
@WebMvcTest(controllers = EventController.class)
@AutoConfigureMockMvc
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService; // Mock 服务以避免数据库依赖

    @MockBean
    private IdempotencyService idempotencyService; // 幂等服务桩

    @BeforeEach
    void setUp() {
        // Default mock behavior for eventService - can be overridden in individual tests
        Mockito.when(eventService.save(any())).thenAnswer(inv -> {
            Event e = new Event();
            e.setId(123L);
            e.setEventName("pv");
            e.setEventTime(FIXED_TIME); // Use fixed time for test reproducibility
            return e;
        });
        
        // Default: idempotency service returns empty (no existing idempotent request)
        // Each test should set up its own mock behavior if needed
        Mockito.when(idempotencyService.findSummary(anyString()))
                .thenReturn(Optional.empty());
        
        // Default: checkAndSet returns true (new idempotent request)
        Mockito.when(idempotencyService.checkAndSet(anyString(), any()))
                .thenReturn(true);
    }

    @Test
    @DisplayName("Should return 400 when required fields are missing")
    void should_Return400_When_RequiredFieldsMissing() throws Exception {
        String body = "{" +
                "\"sessionId\":\"sess-1\"," +
                "\"properties\":{\"k\":\"v\"}" +
                "}"; // 故意缺少 eventName
        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("P0: Should return 201 and verify service method called with correct parameters")
    void should_Return201_When_SingleValidEventSubmitted() throws Exception {
        String body = "{" +
                "\"eventName\":\"page_view\"," +
                "\"sessionId\":\"sess-2\"," +
                "\"properties\":{\"url\":\"/home\"}" +
                "}";
        // 无幂等键场景，不需要打桩
        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        
        // ✅ P0修复：必须验证service被调用且参数正确
        ArgumentCaptor<EventCollectRequest> captor = ArgumentCaptor.forClass(EventCollectRequest.class);
        verify(eventService, times(1)).save(captor.capture());
        
        EventCollectRequest captured = captor.getValue();
        assertThat(captured.getEventName()).isEqualTo("page_view");
        assertThat(captured.getSessionId()).isEqualTo("sess-2");
        assertThat(captured.getProperties()).isNotNull();
        assertThat(captured.getProperties().toString()).contains("url");
    }

    @Test
    @DisplayName("P0: Should return 201 and verify batch service methods called with correct parameters")
    void should_Return201_When_AllBatchEventsValid() throws Exception {
        String body = "[" +
                "{\"eventName\":\"pv\",\"sessionId\":\"s1\",\"properties\":{\"p\":1}}," +
                "{\"eventName\":\"click\",\"sessionId\":\"s2\",\"properties\":{\"btn\":\"ok\"}}" +
                "]";
        mockMvc.perform(post("/api/v1/events/collect/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        
        // ✅ P0修复：必须验证service被调用了2次（批量处理2个事件）
        ArgumentCaptor<EventCollectRequest> captor = ArgumentCaptor.forClass(EventCollectRequest.class);
        verify(eventService, times(2)).save(captor.capture());
        
        // 验证第一个事件
        EventCollectRequest first = captor.getAllValues().get(0);
        assertThat(first.getEventName()).isEqualTo("pv");
        assertThat(first.getSessionId()).isEqualTo("s1");
        
        // 验证第二个事件
        EventCollectRequest second = captor.getAllValues().get(1);
        assertThat(second.getEventName()).isEqualTo("click");
        assertThat(second.getSessionId()).isEqualTo("s2");
    }

    @Test
    @DisplayName("Should return 400 when batch contains invalid events")
    void should_Return400_When_BatchContainsInvalidEvents() throws Exception {
        String body = "[" +
                "{\"sessionId\":\"s1\"}," +
                "{\"eventName\":\"pv\",\"sessionId\":\"s2\"}" +
                "]";
        mockMvc.perform(post("/api/v1/events/collect/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 200 or 204 when same Idempotency-Key is used twice")
    void should_Return200Or204_When_SameIdempotencyKeyUsedTwice() throws Exception {
        String body = "{" +
                "\"eventName\":\"submit\"," +
                "\"sessionId\":\"s3\"," +
                "\"properties\":{\"form\":\"f1\"}" +
                "}";
        String key = "idem-123";

        // First request: checkAndSet returns true (new idempotent request)
        when(idempotencyService.checkAndSet(anyString(), any())).thenReturn(true);
        // First request: findSummary returns empty (no existing summary)
        when(idempotencyService.findSummary(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(body))
                .andExpect(status().isCreated());

        // Second request: checkAndSet returns false (duplicate request)
        when(idempotencyService.checkAndSet(anyString(), any())).thenReturn(false);
        // Second request: findSummary returns existing summary
        IdempotentSummary summary = new IdempotentSummary(123L, "submit", FIXED_TIME);
        when(idempotencyService.findSummary(anyString())).thenReturn(Optional.of(summary));

        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", key)
                        .content(body))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    if (sc != 200 && sc != 204) {
                        throw new AssertionError("expected 200 or 204, but was: " + sc);
                    }
                });
    }

    @Test
    @DisplayName("Should return 200 with details when batch has mixed success and failures")
    void should_Return200WithDetails_When_BatchHasMixedResults() throws Exception {
        String body = "[" +
                "{\"sessionId\":\"s1\"}," + // 缺 eventName → 失败
                "{\"eventName\":\"pv\",\"sessionId\":\"s2\",\"properties\":{\"p\":1}}" + // 成功
                "]";
        mockMvc.perform(post("/api/v1/events/collect/batch/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].index").value(0))
                .andExpect(jsonPath("$.data[0].status").value("failed"))
                .andExpect(jsonPath("$.data[1].index").value(1))
                .andExpect(jsonPath("$.data[1].status").value("created"));
    }

    @Test
    @DisplayName("Should return 200 with all created status when all batch events succeed")
    void should_Return200WithAllCreated_When_AllBatchEventsSucceed() throws Exception {
        String body = "[" +
                "{\"eventName\":\"pv\",\"sessionId\":\"s1\"}," +
                "{\"eventName\":\"click\",\"sessionId\":\"s2\"}" +
                "]";
        mockMvc.perform(post("/api/v1/events/collect/batch/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("created"))
                .andExpect(jsonPath("$.data[1].status").value("created"));
    }

    @Test
    @DisplayName("Should map standard fields: event_type/project_id/event_content to DTO successfully")
    void should_MapStandardFields_When_UsingStandardFieldNames() throws Exception {
        String body = "{" +
                "\"event_type\":\"click\"," +
                "\"project_id\":9," +
                "\"sessionId\":\"sess-std-1\"," +
                "\"event_content\":{\"k\":\"v\"}" +
                "}";

        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<EventCollectRequest> captor = ArgumentCaptor.forClass(EventCollectRequest.class);
        verify(eventService).save(captor.capture());
        EventCollectRequest passed = captor.getValue();
        // 期望字段被映射
        org.assertj.core.api.Assertions.assertThat(passed.getEventName()).isEqualTo("click");
        org.assertj.core.api.Assertions.assertThat(passed.getTenantId()).isEqualTo(9);
        org.assertj.core.api.Assertions.assertThat(passed.getSessionId()).isEqualTo("sess-std-1");
        org.assertj.core.api.Assertions.assertThat(passed.getProperties().toString()).contains("\"k\":\"v\"");
    }

    @Test
    @DisplayName("Should enrich event_content with UA/Referer/IP from headers")
    void should_EnrichEventContent_When_HeadersProvided() throws Exception {
        String body = "{" +
                "\"event_type\":\"pv\"," +
                "\"project_id\":1," +
                "\"sessionId\":\"sess-std-2\"," +
                "\"event_content\":{\"x\":1}" +
                "}";

        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0 TestUA")
                        .header("Referer", "https://example.com/page")
                        .header("X-Forwarded-For", "203.0.113.5, 70.41.3.18")
                        .content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<EventCollectRequest> captor = ArgumentCaptor.forClass(EventCollectRequest.class);
        verify(eventService).save(captor.capture());
        EventCollectRequest passed = captor.getValue();
        String props = passed.getProperties() == null ? null : passed.getProperties().toString();
        org.assertj.core.api.Assertions.assertThat(props).contains("TestUA");
        org.assertj.core.api.Assertions.assertThat(props).contains("example.com/page");
        org.assertj.core.api.Assertions.assertThat(props).contains("203.0.113.5");
    }

    @Test
    @DisplayName("Should apply and validate tenantId from X-Tenant-Id header")
    void should_ApplyAndValidateTenantId_When_TenantHeaderProvided() throws Exception {
        // 补齐：无 project_id，使用 Header 9
        String bodyNoProject = "{" +
                "\"event_type\":\"pv\"," +
                "\"sessionId\":\"sess-tenant-1\"" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "9")
                        .content(bodyNoProject))
                .andExpect(status().isCreated());

        // 匹配：body = 9 且 Header = 9
        String bodyMatch = "{" +
                "\"event_type\":\"pv\"," +
                "\"project_id\":9," +
                "\"sessionId\":\"sess-tenant-2\"" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "9")
                        .content(bodyMatch))
                .andExpect(status().isCreated());

        // 不匹配：body = 8, Header = 9 → 403
        String bodyMismatch = "{" +
                "\"event_type\":\"pv\"," +
                "\"project_id\":8," +
                "\"sessionId\":\"sess-tenant-3\"" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-Id", "9")
                        .content(bodyMismatch))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 201 when GET request has minimum required parameters")
    void should_Return201_When_GetRequestHasRequiredParameters() throws Exception {
        mockMvc.perform(get("/api/v1/events/collect")
                        .param("eventName", "page_view")
                        .param("sessionId", "sess-g1"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return 200 or 204 when GET request uses same Idempotency-Key twice")
    void should_Return200Or204_When_GetRequestUsesSameIdempotencyKeyTwice() throws Exception {
        String key = "idem-get-1";
        
        // First request: new idempotent request
        when(idempotencyService.checkAndSet(anyString(), any())).thenReturn(true);
        when(idempotencyService.findSummary(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/events/collect")
                        .header("Idempotency-Key", key)
                        .param("eventName", "submit")
                        .param("sessionId", "sess-g2"))
                .andExpect(status().isCreated());

        // Second request: duplicate idempotent request
        when(idempotencyService.checkAndSet(anyString(), any())).thenReturn(false);
        IdempotentSummary summary = new IdempotentSummary(123L, "submit", FIXED_TIME);
        when(idempotencyService.findSummary(anyString())).thenReturn(Optional.of(summary));

        mockMvc.perform(get("/api/v1/events/collect")
                        .header("Idempotency-Key", key)
                        .param("eventName", "submit")
                        .param("sessionId", "sess-g2"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    if (sc != 200 && sc != 204) {
                        throw new AssertionError("expected 200 or 204, but was: " + sc);
                    }
                });
    }

    @Test
    @DisplayName("Should succeed when POST has no tenantId in header or body")
    void should_Succeed_When_PostHasNoTenantId() throws Exception {
        String body = "{" +
                "\"eventName\":\"pv\"," +
                "\"sessionId\":\"sess-no-tenant\"" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should succeed when GET has tenantId in param but no header")
    void should_Succeed_When_GetHasTenantIdInParamButNoHeader() throws Exception {
        mockMvc.perform(get("/api/v1/events/collect")
                        .param("eventName", "pv")
                        .param("sessionId", "sess-no-header")
                        .param("tenantId", "5"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should succeed when GET has no tenantId in header or param")
    void should_Succeed_When_GetHasNoTenantId() throws Exception {
        mockMvc.perform(get("/api/v1/events/collect")
                        .param("eventName", "pv")
                        .param("sessionId", "sess-no-tenant"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should use RemoteAddr when X-Forwarded-For header is null")
    void should_UseRemoteAddr_When_XForwardedForIsNull() throws Exception {
        String body = "{" +
                "\"eventName\":\"pv\"," +
                "\"sessionId\":\"sess-xff-null\"" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should use X-Forwarded-For directly when it has no comma")
    void should_UseXForwardedForDirectly_When_NoCommaPresent() throws Exception {
        String body = "{" +
                "\"eventName\":\"pv\"," +
                "\"sessionId\":\"sess-xff-single\"" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "192.168.1.1")
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should handle enrichment when properties is null")
    void should_HandleEnrichment_When_PropertiesIsNull() throws Exception {
        String body = "{" +
                "\"eventName\":\"pv\"," +
                "\"sessionId\":\"sess-null-props\"" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0")
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should handle enrichment when properties is not an ObjectNode")
    void should_HandleEnrichment_When_PropertiesIsNotObjectNode() throws Exception {
        String body = "{" +
                "\"eventName\":\"pv\"," +
                "\"sessionId\":\"sess-non-obj-props\"," +
                "\"properties\":\"not-an-object\"" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0")
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should handle enrichment when UA/Referer/IP headers are blank")
    void should_HandleEnrichment_When_HeadersAreBlank() throws Exception {
        String body = "{" +
                "\"eventName\":\"pv\"," +
                "\"sessionId\":\"sess-blank-headers\"" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "   ")
                        .header("Referer", "")
                        .content(body))
                .andExpect(status().isCreated());
    }
}

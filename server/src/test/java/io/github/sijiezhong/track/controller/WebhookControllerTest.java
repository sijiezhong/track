package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.domain.WebhookSubscription;
import io.github.sijiezhong.track.repository.WebhookSubscriptionRepository;
import io.github.sijiezhong.track.service.WebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for WebhookController (WebMvc layer, no database dependency).
 * 
 * Coverage includes:
 * - Request validation
 * - Tenant ID validation
 * - Permission checks
 * - Service method calls with correct parameters
 * 
 * Note: Integration tests are in WebhookSubscriptionIntegrationTest.
 */
@WebMvcTest(controllers = WebhookController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit test
public class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WebhookSubscriptionRepository repository;

    @MockBean
    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        // Default mock behavior can be overridden in individual tests
    }

    @Test
    @DisplayName("P0: Should create subscription and verify service called with correct parameters")
    void should_CreateSubscription_And_VerifyServiceCalled() throws Exception {
        // Given: Valid subscription request
        String body = "{\"url\":\"https://example.com/webhook\",\"enabled\":true}";
        
        // Mock repository save
        WebhookSubscription saved = new WebhookSubscription();
        saved.setId(1L);
        saved.setUrl("https://example.com/webhook");
        saved.setTenantId(1);
        saved.setEnabled(true);
        when(repository.save(any(WebhookSubscription.class))).thenReturn(saved);

        // When: Create subscription
        mockMvc.perform(post("/api/v1/webhooks")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // ✅ P0修复：必须验证repository.save()被调用且参数正确
        org.mockito.ArgumentCaptor<WebhookSubscription> captor = 
                org.mockito.ArgumentCaptor.forClass(WebhookSubscription.class);
        verify(repository).save(captor.capture());

        WebhookSubscription captured = captor.getValue();
        assertThat(captured.getUrl()).isEqualTo("https://example.com/webhook");
        assertThat(captured.getTenantId()).isEqualTo(1);
        assertThat(captured.getEnabled()).isTrue();
        assertThat(captured.getCreateTime()).isNotNull();
        assertThat(captured.getUpdateTime()).isNotNull();
    }

    @Test
    @DisplayName("P0: Should reject subscription when tenantId in body mismatches header")
    void should_RejectSubscription_When_TenantIdMismatch() throws Exception {
        // Given: Subscription with tenantId=2, but header has tenantId=1
        String body = "{\"url\":\"https://example.com/webhook\",\"tenantId\":2}";

        // When: Create subscription
        mockMvc.perform(post("/api/v1/webhooks")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        // Then: Repository should NOT be called
        verify(repository, never()).save(any(WebhookSubscription.class));
    }

    @Test
    @DisplayName("P0: Should set default enabled=true when not provided")
    void should_SetDefaultEnabled_When_NotProvided() throws Exception {
        // Given: Subscription without enabled field
        String body = "{\"url\":\"https://example.com/webhook\"}";

        WebhookSubscription saved = new WebhookSubscription();
        saved.setId(1L);
        when(repository.save(any(WebhookSubscription.class))).thenReturn(saved);

        // When: Create subscription
        mockMvc.perform(post("/api/v1/webhooks")
                        .header("X-Tenant-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Then: Verify enabled is set to true by default
        org.mockito.ArgumentCaptor<WebhookSubscription> captor = 
                org.mockito.ArgumentCaptor.forClass(WebhookSubscription.class);
        verify(repository).save(captor.capture());

        WebhookSubscription captured = captor.getValue();
        assertThat(captured.getEnabled()).isTrue(); // ✅ 验证默认值
    }

    @Test
    @DisplayName("P0: Should call webhookService.replayLatest with correct tenantId")
    void should_CallWebhookServiceReplayLatest_WithCorrectTenantId() throws Exception {
        // When: Replay latest events
        mockMvc.perform(post("/api/v1/webhooks/replay/latest")
                        .header("X-Tenant-Id", "1"))
                .andExpect(status().isOk());

        // ✅ P0修复：必须验证webhookService.replayLatest()被调用且参数正确
        verify(webhookService).replayLatest(eq(1));
    }

    @Test
    @DisplayName("P0: Should handle tenantId extraction correctly for replay")
    void should_HandleTenantIdExtraction_ForReplay() throws Exception {
        // When: Replay with different tenantId
        mockMvc.perform(post("/api/v1/webhooks/replay/latest")
                        .header("X-Tenant-Id", "5"))
                .andExpect(status().isOk());

        // Then: Verify correct tenantId is passed
        verify(webhookService).replayLatest(eq(5));
    }
}


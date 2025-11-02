package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.WebhookSubscription;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.repository.WebhookSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookService.
 * 
 * Coverage includes:
 * - Legacy webhook posting
 * - Subscription webhook posting with/without signatures
 * - Retry logic on failures
 * - Replay latest events functionality
 */
public class WebhookServiceTest {

  private RestTemplate restTemplate;
  private WebhookSettings settings;
  private WebhookSubscriptionRepository subscriptionRepository;
  private EventRepository eventRepository;
  private WebhookService webhookService;

  @BeforeEach
  void setUp() {
    restTemplate = mock(RestTemplate.class);
    settings = new WebhookSettings();
    subscriptionRepository = mock(WebhookSubscriptionRepository.class);
    eventRepository = mock(EventRepository.class);
    webhookService = new WebhookService(restTemplate, settings, subscriptionRepository, eventRepository);
  }

  @Test
  @DisplayName("Should post event to legacy URL when legacy webhook is enabled")
  void should_PostToLegacyUrl_When_LegacyWebhookEnabled() {
    settings.setEnabled(true);
    settings.setUrl("https://legacy.example/hook");
    Event event = buildEvent();

    webhookService.onEvent(event);

    // Should call exactly once for legacy webhook (no subscriptions in this test)
    verify(restTemplate, times(1)).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class),
        eq(String.class));
  }

  @Test
  @DisplayName("Should add signature header when subscription webhook has secret")
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void should_AddSignatureHeader_When_SubscriptionWebhookHasSecret() {
    settings.setEnabled(false);
    WebhookSubscription subscription = new WebhookSubscription();
    subscription.setUrl("https://subs.example/h");
    subscription.setSecret("secret-key");
    when(subscriptionRepository.findByTenantIdAndEnabledTrue(1)).thenReturn(List.of(subscription));

    Event event = buildEvent();
    webhookService.onEvent(event);

    ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass((Class) HttpEntity.class);
    verify(restTemplate).postForEntity(eq("https://subs.example/h"), captor.capture(), eq(String.class));
    HttpEntity<String> entity = captor.getValue();
    assertThat(entity.getHeaders().getFirst("X-Webhook-Signature")).isNotNull();
    assertThat(entity.getHeaders().getFirst("X-Webhook-Signature")).isNotEmpty();
  }

  @Test
  @DisplayName("Should not add signature header when subscription webhook secret is null")
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void should_NotAddSignatureHeader_When_SubscriptionWebhookSecretIsNull() {
    settings.setEnabled(false);
    WebhookSubscription subscription = new WebhookSubscription();
    subscription.setUrl("https://subs.example/h");
    subscription.setSecret(null);
    when(subscriptionRepository.findByTenantIdAndEnabledTrue(1)).thenReturn(List.of(subscription));

    Event event = buildEvent();
    webhookService.onEvent(event);

    ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass((Class) HttpEntity.class);
    verify(restTemplate).postForEntity(eq("https://subs.example/h"), captor.capture(), eq(String.class));
    HttpEntity<String> entity = captor.getValue();
    assertThat(entity.getHeaders().getFirst("X-Webhook-Signature")).isNull();
  }

  @Test
  @DisplayName("Should not add signature header when subscription webhook secret is empty")
  @SuppressWarnings({ "rawtypes", "unchecked" })
  void should_NotAddSignatureHeader_When_SubscriptionWebhookSecretIsEmpty() {
    settings.setEnabled(false);
    WebhookSubscription subscription = new WebhookSubscription();
    subscription.setUrl("https://subs.example/h");
    subscription.setSecret("");
    subscription.setEnabled(true);
    when(subscriptionRepository.findByTenantIdAndEnabledTrue(1)).thenReturn(List.of(subscription));

    Event event = buildEvent();
    webhookService.onEvent(event);

    ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass((Class) HttpEntity.class);
    verify(restTemplate).postForEntity(eq("https://subs.example/h"), captor.capture(), eq(String.class));
    HttpEntity<String> entity = captor.getValue();
    assertThat(entity.getHeaders().containsKey("X-Webhook-Signature")).isFalse();
  }

  @Test
  @DisplayName("Should retry once when first push fails")
  void should_RetryOnce_When_FirstPushFails() {
    settings.setEnabled(true);
    settings.setUrl("https://legacy.example/hook");
    doThrow(new RuntimeException("network error"))
        .doReturn(org.springframework.http.ResponseEntity.ok("ok"))
        .when(restTemplate).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class));
    Event event = buildEvent();
    webhookService.onEvent(event);
    verify(restTemplate, times(2)).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class),
        eq(String.class));
  }

  @Test
  @DisplayName("Should attempt twice without throwing exception when both pushes fail")
  void should_AttemptTwice_When_BothPushesFail() {
    settings.setEnabled(true);
    settings.setUrl("https://legacy.example/hook");
    doThrow(new RuntimeException("network error"))
        .when(restTemplate).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class));
    Event event = buildEvent();
    webhookService.onEvent(event);
    // Should retry once, so called twice total
    verify(restTemplate, times(2)).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class),
        eq(String.class));
  }

  @Test
  @DisplayName("Should send to both legacy and subscription webhooks with signature and retry logic")
  void should_SendToLegacyAndSubscriptions_When_BothEnabled() {
    // Business scenario: Hybrid webhook deployment
    // System supports both legacy global webhook and per-tenant subscription webhooks.
    // This test verifies that when both are enabled, events are sent to both endpoints
    // with appropriate retry logic for legacy and signature headers for subscriptions.
    
    // Given: Legacy webhook is enabled
    settings.setEnabled(true);
    settings.setUrl("https://legacy.example/hook");

    // Given: Tenant has an active subscription webhook with secret
    WebhookSubscription subscription = new WebhookSubscription();
    subscription.setTenantId(1);
    subscription.setUrl("https://sub.example/h1");
    subscription.setSecret("secret1");
    subscription.setEnabled(true);
    when(subscriptionRepository.findByTenantIdAndEnabledTrue(1)).thenReturn(List.of(subscription));

    // Given: An event occurs
    Event event = new Event();
    event.setId(100L);
    event.setEventName("pv");
    event.setTenantId(1);

    // Mock: Legacy webhook fails first time, succeeds on retry
    // This tests the retry mechanism for legacy webhooks
    when(restTemplate.postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class)))
        .thenThrow(new RuntimeException("fail"))
        .thenReturn(org.springframework.http.ResponseEntity.ok("ok"));

    // Mock: Subscription webhook succeeds immediately
    when(restTemplate.postForEntity(eq("https://sub.example/h1"), any(HttpEntity.class), eq(String.class)))
        .thenReturn(org.springframework.http.ResponseEntity.ok("ok"));

    // When: Event is processed
    webhookService.onEvent(event);

    // Then: Legacy webhook should be called twice (initial + retry)
    verify(restTemplate, times(2))
        .postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class));

    // Then: Subscription webhook should be called once with signature header
    // The signature header is critical for webhook security - it allows the receiver
    // to verify the request came from our system and hasn't been tampered with.
    ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass((Class) HttpEntity.class);
    verify(restTemplate).postForEntity(eq("https://sub.example/h1"), entityCaptor.capture(), eq(String.class));
    HttpEntity<String> captured = entityCaptor.getValue();
    assertThat(captured).isNotNull();
    assertThat(captured.getHeaders().containsKey("X-Webhook-Signature")).isTrue();
    String signature = captured.getHeaders().getFirst("X-Webhook-Signature");
    assertThat(signature).isNotNull();
    assertThat(signature).isNotEmpty();
  }

  @ParameterizedTest
  @ValueSource(ints = { 2, 99 })
  @DisplayName("Should not perform any webhook calls when no latest events exist for tenant")
  void should_NotPerformWebhookCalls_When_NoLatestEventsExist(int tenantId) {
    // Given: repository returns empty list for the tenant
    when(eventRepository.findLatestByTenant(tenantId)).thenReturn(Collections.emptyList());
    
    // When: replayLatest is called
    webhookService.replayLatest(tenantId);
    
    // Then: no webhook calls should be made
    verify(restTemplate, never()).postForEntity(anyString(), any(), eq(String.class));
    verifyNoInteractions(restTemplate);
  }

  @Test
  @DisplayName("Should handle HTTP 500 server errors gracefully without retry")
  void should_HandleHttp500Error_WithoutRetry() {
    // Business scenario: Webhook endpoint returns server error
    // Note: Current implementation only retries on exceptions, not on HTTP error status codes.
    // HTTP error responses (like 500) are considered successful from RestTemplate's perspective
    // and do not trigger the retry mechanism.
    
    settings.setEnabled(true);
    settings.setUrl("https://legacy.example/hook");
    Event event = buildEvent();

    // Mock: Call returns HTTP 500 response (not an exception)
    org.springframework.http.ResponseEntity<String> error500 = org.springframework.http.ResponseEntity.status(500).body("Internal Server Error");
    when(restTemplate.postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class)))
        .thenReturn(error500);

    // When: Event is processed
    webhookService.onEvent(event);

    // Then: Should attempt once (no retry for HTTP status codes, only for exceptions)
    verify(restTemplate, times(1)).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class));
  }

  @Test
  @DisplayName("Should handle HTTP 404 not found errors gracefully without retry")
  void should_HandleHttp404Error_WithoutRetry() {
    // Business scenario: Webhook endpoint not found
    // Note: Current implementation only retries on exceptions, not on HTTP error status codes.
    // HTTP error responses (like 404) are considered successful from RestTemplate's perspective.
    
    settings.setEnabled(true);
    settings.setUrl("https://legacy.example/hook");
    Event event = buildEvent();

    // Mock: Call returns HTTP 404 response (not an exception)
    org.springframework.http.ResponseEntity<String> error404 = org.springframework.http.ResponseEntity.status(404).body("Not Found");
    when(restTemplate.postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class)))
        .thenReturn(error404);

    // When: Event is processed
    webhookService.onEvent(event);

    // Then: Should attempt once (no retry for HTTP status codes)
    verify(restTemplate, times(1)).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class));
  }

  @Test
  @DisplayName("Should handle HTTP 500 errors thrown as exceptions with retry")
  void should_HandleHttp500Exception_WithRetry() {
    // Business scenario: Webhook endpoint throws HttpServerErrorException for 500 error
    // When RestTemplate throws an exception (rather than returning error response),
    // the retry mechanism should be triggered.
    
    settings.setEnabled(true);
    settings.setUrl("https://legacy.example/hook");
    Event event = buildEvent();

    // Mock: RestTemplate throws HttpServerErrorException for 500
    org.springframework.web.client.HttpServerErrorException serverError = 
        new org.springframework.web.client.HttpServerErrorException(
            org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error");
    
    doThrow(serverError)
        .doThrow(serverError)
        .when(restTemplate).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class));

    // When: Event is processed
    webhookService.onEvent(event);

    // Then: Should attempt twice (initial + retry)
    verify(restTemplate, times(2)).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class));
  }

  @Test
  @DisplayName("Should handle network timeout exceptions with retry")
  void should_HandleNetworkTimeout_WithRetry() {
    // Business scenario: Network timeout (connection timeout or read timeout)
    // Timeouts are often transient network issues, so retry makes sense.
    
    settings.setEnabled(true);
    settings.setUrl("https://legacy.example/hook");
    Event event = buildEvent();

    // Mock: Simulate network timeout (RestClientException or subclasses)
    org.springframework.web.client.ResourceAccessException timeoutException = 
        new org.springframework.web.client.ResourceAccessException("Read timed out");
    doThrow(timeoutException)
        .doThrow(timeoutException)
        .when(restTemplate).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class));

    // When: Event is processed
    webhookService.onEvent(event);

    // Then: Should attempt twice (initial + retry)
    verify(restTemplate, times(2)).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class));
  }

  @Test
  @DisplayName("Should handle malformed URL gracefully")
  void should_HandleMalformedUrl_Gracefully() {
    // Business scenario: Invalid URL format in webhook configuration
    // System should not crash, but silently fail (catch exception).
    
    settings.setEnabled(true);
    settings.setUrl("not-a-valid-url");
    Event event = buildEvent();

    // Mock: RestTemplate throws exception for invalid URL
    doThrow(new IllegalArgumentException("Invalid URL format"))
        .when(restTemplate).postForEntity(eq("not-a-valid-url"), any(HttpEntity.class), eq(String.class));

    // When: Event is processed
    // Expected: Should not throw exception, should attempt retry and then silently fail
    webhookService.onEvent(event);

    // Then: Should attempt twice (initial + retry attempt)
    verify(restTemplate, times(2)).postForEntity(eq("not-a-valid-url"), any(HttpEntity.class), eq(String.class));
  }

  @Test
  @DisplayName("Should handle concurrent webhook pushes to multiple subscriptions")
  void should_HandleConcurrentWebhookPushes_ToMultipleSubscriptions() {
    // Business scenario: Tenant has multiple active webhook subscriptions
    // System should send to all subscriptions concurrently, handling failures independently.
    
    settings.setEnabled(false);
    
    // Given: Tenant has 3 active subscriptions
    WebhookSubscription sub1 = new WebhookSubscription();
    sub1.setUrl("https://sub1.example/hook");
    sub1.setSecret("secret1");
    sub1.setEnabled(true);
    
    WebhookSubscription sub2 = new WebhookSubscription();
    sub2.setUrl("https://sub2.example/hook");
    sub2.setSecret("secret2");
    sub2.setEnabled(true);
    
    WebhookSubscription sub3 = new WebhookSubscription();
    sub3.setUrl("https://sub3.example/hook");
    sub3.setSecret(null); // No secret
    sub3.setEnabled(true);
    
    when(subscriptionRepository.findByTenantIdAndEnabledTrue(1)).thenReturn(List.of(sub1, sub2, sub3));

    Event event = buildEvent();

    // Mock: sub2 throws exception (triggers retry), others succeed
    org.springframework.web.client.HttpServerErrorException serverError = 
        new org.springframework.web.client.HttpServerErrorException(
            org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error");
    
    when(restTemplate.postForEntity(eq("https://sub1.example/hook"), any(HttpEntity.class), eq(String.class)))
        .thenReturn(org.springframework.http.ResponseEntity.ok("ok"));
    doThrow(serverError)
        .doThrow(serverError)
        .when(restTemplate).postForEntity(eq("https://sub2.example/hook"), any(HttpEntity.class), eq(String.class));
    when(restTemplate.postForEntity(eq("https://sub3.example/hook"), any(HttpEntity.class), eq(String.class)))
        .thenReturn(org.springframework.http.ResponseEntity.ok("ok"));

    // When: Event is processed
    webhookService.onEvent(event);

    // Then: All subscriptions should be attempted
    // sub1: 1 call (success)
    verify(restTemplate, times(1)).postForEntity(eq("https://sub1.example/hook"), any(HttpEntity.class), eq(String.class));
    // sub2: 2 calls (failed, retried, failed again)
    verify(restTemplate, times(2)).postForEntity(eq("https://sub2.example/hook"), any(HttpEntity.class), eq(String.class));
    // sub3: 1 call (success, no signature)
    verify(restTemplate, times(1)).postForEntity(eq("https://sub3.example/hook"), any(HttpEntity.class), eq(String.class));
  }

  @Test
  @DisplayName("Should handle retry failure when second attempt also fails")
  void should_HandleRetryFailure_WhenSecondAttemptAlsoFails() {
    // Business scenario: Both initial push and retry fail with different exceptions
    // System should gracefully handle complete failure without throwing exceptions.
    
    settings.setEnabled(true);
    settings.setUrl("https://legacy.example/hook");
    Event event = buildEvent();

    // Mock: First attempt throws RuntimeException, retry throws HttpServerErrorException
    org.springframework.web.client.HttpServerErrorException serverError = 
        new org.springframework.web.client.HttpServerErrorException(
            org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
            "Service Unavailable");
    
    doThrow(new RuntimeException("Network error"))
        .doThrow(serverError)
        .when(restTemplate).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class));

    // When: Event is processed
    // Expected: Should not throw exception, both attempts should be made
    webhookService.onEvent(event);

    // Then: Should attempt twice despite both failures
    verify(restTemplate, times(2)).postForEntity(eq("https://legacy.example/hook"), any(HttpEntity.class), eq(String.class));
  }

  private Event buildEvent() {
    Event event = new Event();
    event.setId(1L);
    event.setEventName("pv");
    event.setTenantId(1);
    return event;
  }
}

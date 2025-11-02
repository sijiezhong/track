package io.github.sijiezhong.track.repository;

import io.github.sijiezhong.track.domain.WebhookSubscription;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for WebhookSubscriptionRepository custom query methods.
 * 
 * Coverage includes:
 * - findByTenantIdAndEnabledTrue with existing enabled subscriptions
 * - findByTenantIdAndEnabledTrue excluding disabled subscriptions
 * - findByTenantIdAndEnabledTrue with no enabled subscriptions
 * - Edge cases (different tenants, null enabled flag)
 */
public class WebhookSubscriptionRepositoryTest extends PostgresTestBase {

    @Autowired
    private WebhookSubscriptionRepository webhookSubscriptionRepository;

    @Test
    @DisplayName("Should find enabled subscriptions by tenantId when subscriptions exist")
    void should_FindByTenantIdAndEnabledTrue_When_SubscriptionsExist() {
        // Given: Create enabled and disabled subscriptions for tenant 1
        WebhookSubscription enabled1 = createSubscription(1, "https://webhook1.example.com", true);
        webhookSubscriptionRepository.save(enabled1);

        WebhookSubscription enabled2 = createSubscription(1, "https://webhook2.example.com", true);
        webhookSubscriptionRepository.save(enabled2);

        WebhookSubscription disabled = createSubscription(1, "https://webhook3.example.com", false);
        webhookSubscriptionRepository.save(disabled);

        // When: Find enabled subscriptions for tenant 1
        List<WebhookSubscription> found = webhookSubscriptionRepository.findByTenantIdAndEnabledTrue(1);

        // Then: Should only return enabled subscriptions
        assertThat(found).hasSize(2);
        assertThat(found).isNotEmpty(); // Explicit non-empty check before allMatch
        assertThat(found).extracting(WebhookSubscription::getUrl)
                .containsExactlyInAnyOrder("https://webhook1.example.com", "https://webhook2.example.com");
        assertThat(found).allMatch(WebhookSubscription::getEnabled);
        assertThat(found).allMatch(sub -> sub.getTenantId().equals(1));
    }

    @Test
    @DisplayName("Should exclude disabled subscriptions when finding enabled ones")
    void should_ExcludeDisabled_When_FindingEnabled() {
        // Given: Create enabled and disabled subscriptions
        WebhookSubscription enabled = createSubscription(1, "https://enabled.example.com", true);
        webhookSubscriptionRepository.save(enabled);

        WebhookSubscription disabled1 = createSubscription(1, "https://disabled1.example.com", false);
        webhookSubscriptionRepository.save(disabled1);

        WebhookSubscription disabled2 = createSubscription(1, "https://disabled2.example.com", false);
        webhookSubscriptionRepository.save(disabled2);

        // When: Find enabled subscriptions
        List<WebhookSubscription> found = webhookSubscriptionRepository.findByTenantIdAndEnabledTrue(1);

        // Then: Should only return the enabled one
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getUrl()).isEqualTo("https://enabled.example.com");
        assertThat(found.get(0).getEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should return empty list when no enabled subscriptions exist")
    void should_ReturnEmpty_When_NoEnabledSubscriptions() {
        // Given: Create only disabled subscriptions for tenant 1
        WebhookSubscription disabled1 = createSubscription(1, "https://disabled1.example.com", false);
        webhookSubscriptionRepository.save(disabled1);

        WebhookSubscription disabled2 = createSubscription(1, "https://disabled2.example.com", false);
        webhookSubscriptionRepository.save(disabled2);

        // When: Find enabled subscriptions
        List<WebhookSubscription> found = webhookSubscriptionRepository.findByTenantIdAndEnabledTrue(1);

        // Then: Should return empty list
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when tenant has no subscriptions")
    void should_ReturnEmpty_When_TenantHasNoSubscriptions() {
        // Given: Create subscriptions for tenant 2 (not tenant 1)
        WebhookSubscription otherTenant = createSubscription(2, "https://other.example.com", true);
        webhookSubscriptionRepository.save(otherTenant);

        // When: Find enabled subscriptions for tenant 1
        List<WebhookSubscription> found = webhookSubscriptionRepository.findByTenantIdAndEnabledTrue(1);

        // Then: Should return empty list
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find subscriptions only for specified tenant")
    void should_FindSubscriptionsOnlyForSpecifiedTenant() {
        // Given: Create subscriptions for different tenants
        WebhookSubscription tenant1Enabled1 = createSubscription(1, "https://tenant1-1.example.com", true);
        webhookSubscriptionRepository.save(tenant1Enabled1);

        WebhookSubscription tenant1Enabled2 = createSubscription(1, "https://tenant1-2.example.com", true);
        webhookSubscriptionRepository.save(tenant1Enabled2);

        WebhookSubscription tenant2Enabled = createSubscription(2, "https://tenant2.example.com", true);
        webhookSubscriptionRepository.save(tenant2Enabled);

        WebhookSubscription tenant3Enabled = createSubscription(3, "https://tenant3.example.com", true);
        webhookSubscriptionRepository.save(tenant3Enabled);

        // When: Find enabled subscriptions for tenant 1
        List<WebhookSubscription> found = webhookSubscriptionRepository.findByTenantIdAndEnabledTrue(1);

        // Then: Should only return subscriptions for tenant 1
        assertThat(found).hasSize(2);
        assertThat(found).isNotEmpty(); // Explicit non-empty check before allMatch
        assertThat(found).extracting(WebhookSubscription::getUrl)
                .containsExactlyInAnyOrder("https://tenant1-1.example.com", "https://tenant1-2.example.com");
        assertThat(found).allMatch(sub -> sub.getTenantId().equals(1));
    }

    private WebhookSubscription createSubscription(Integer tenantId, String url, Boolean enabled) {
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setTenantId(tenantId);
        subscription.setUrl(url);
        subscription.setSecret("secret-" + url);
        subscription.setEnabled(enabled);
        return subscription;
    }
}


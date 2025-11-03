package io.github.sijiezhong.track.repository;

import io.github.sijiezhong.track.domain.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {
    List<WebhookSubscription> findByAppIdAndEnabledTrue(Integer appId);
}



package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.domain.Event;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.github.sijiezhong.track.repository.WebhookSubscriptionRepository;
import io.github.sijiezhong.track.domain.WebhookSubscription;
import io.github.sijiezhong.track.repository.EventRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
public class WebhookService {
    private final RestTemplate restTemplate;
    private final WebhookSettings settings;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final EventRepository eventRepository;

    public WebhookService(RestTemplate restTemplate, WebhookSettings settings, WebhookSubscriptionRepository subscriptionRepository, EventRepository eventRepository) {
        this.restTemplate = restTemplate;
        this.settings = settings;
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
    }

    public void onEvent(Event event) {
        // 向 legacy 单目标发送（保持兼容）
        if (settings.isEnabled() && settings.getUrl() != null) {
            tryPostToUrl(settings.getUrl(), null, event);
        }
        // 向当前应用的所有订阅发送
        List<WebhookSubscription> subs = subscriptionRepository.findByAppIdAndEnabledTrue(event.getAppId());
        for (WebhookSubscription s : subs) {
            tryPostToUrl(s.getUrl(), s.getSecret(), event);
        }
    }

    public void replayLatest(Integer appId) {
        List<Event> latestList = eventRepository.findLatestByTenant(appId);
        if (latestList.isEmpty()) return;
        onEvent(latestList.get(0));
    }

    private void tryPostToUrl(String url, String secret, Event event) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String payload = "{\"eventId\":" + event.getId() + ",\"eventName\":\"" + event.getEventName() + "\",\"appId\":" + event.getAppId() + "}";
        if (secret != null && !secret.isEmpty()) {
            headers.set("X-Webhook-Signature", hmacSha256Base64(secret, payload));
        }
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);
        try {
            restTemplate.postForEntity(url, entity, String.class);
        } catch (Exception ignored) {
            try {
                restTemplate.postForEntity(url, entity, String.class);
            } catch (Exception ignored2) {
            }
        }
    }

    private String hmacSha256Base64(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            return "";
        }
    }
}



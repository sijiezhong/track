package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.config.IdempotencyProperties;
import io.github.sijiezhong.track.dto.IdempotentSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static io.github.sijiezhong.track.testsupport.TestConstants.FIXED_TIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisIdempotencyService.
 * 
 * Coverage includes:
 * - Idempotency key check and set operations
 * - TTL configuration
 * - Summary retrieval from Redis
 * - JSON serialization/deserialization
 * - Edge cases (null keys, invalid JSON, null Redis values)
 */
class RedisIdempotencyServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private IdempotencyProperties properties;
    private RedisIdempotencyService service;

    @BeforeEach
    void setUp() {
        redis = Mockito.mock(StringRedisTemplate.class);
        valueOps = Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        properties = new IdempotencyProperties();
        properties.setTtlSeconds(60);
        service = new RedisIdempotencyService(redis, properties);
    }

    @Test
    void checkAndSetShouldBypassWhenKeyNullOrEmpty() {
        IdempotentSummary summary = new IdempotentSummary(1L, "pv", FIXED_TIME);
        assertThat(service.checkAndSet(null, summary)).isTrue();
        assertThat(service.checkAndSet("", summary)).isTrue();
        verify(valueOps, never()).setIfAbsent(any(), any(), any(Duration.class));
    }

    @Test
    void checkAndSetShouldReturnTrueWhenSetIfAbsentSucceeds() {
        IdempotentSummary summary = new IdempotentSummary(2L, "signup", FIXED_TIME);
        when(valueOps.setIfAbsent(eq("idem:k1"), any(String.class), eq(Duration.ofSeconds(60)))).thenReturn(Boolean.TRUE);
        boolean first = service.checkAndSet("k1", summary);
        assertThat(first).isTrue();
        verify(valueOps, times(1)).setIfAbsent(eq("idem:k1"), any(String.class), eq(Duration.ofSeconds(60)));
    }

    @Test
    void checkAndSetShouldReturnFalseWhenSetIfAbsentFails() {
        IdempotentSummary summary = new IdempotentSummary(3L, "order", FIXED_TIME);
        when(valueOps.setIfAbsent(eq("idem:k2"), any(String.class), eq(Duration.ofSeconds(60)))).thenReturn(Boolean.FALSE);
        boolean first = service.checkAndSet("k2", summary);
        assertThat(first).isFalse();
        verify(valueOps, times(1)).setIfAbsent(eq("idem:k2"), any(String.class), eq(Duration.ofSeconds(60)));
    }

    @Test
    void findSummaryShouldReturnEmptyForNullOrEmptyKey() {
        assertThat(service.findSummary(null)).isEmpty();
        assertThat(service.findSummary("")).isEmpty();
        verify(valueOps, never()).get(any());
    }

    @Test
    void findSummaryShouldReturnPresentWhenJsonValid() throws Exception {
        IdempotentSummary summary = new IdempotentSummary(5L, "pv", FIXED_TIME);
        // Serialize using a real service path by calling checkAndSet then reading the argument back
        when(valueOps.setIfAbsent(eq("idem:k3"), any(String.class), any(Duration.class))).thenReturn(Boolean.TRUE);
        service.checkAndSet("k3", summary);
        // Capture the JSON passed to setIfAbsent to reuse as Redis value
        verify(valueOps).setIfAbsent(eq("idem:k3"), any(String.class), eq(Duration.ofSeconds(60)));
        // For simplicity, just return a minimal JSON matching the fields
        when(valueOps.get("idem:k3")).thenReturn("{\"eventId\":5,\"eventName\":\"pv\",\"eventTime\":\"" + summary.getEventTime().toString() + "\"}");
        when(redis.opsForValue()).thenReturn(valueOps);

        Optional<IdempotentSummary> found = service.findSummary("k3");
        assertThat(found).isPresent();
        assertThat(found.get().getEventId()).isEqualTo(5L);
        assertThat(found.get().getEventName()).isEqualTo("pv");
    }

    @Test
    void findSummaryShouldReturnEmptyWhenJsonInvalid() {
        when(valueOps.get("idem:k4")).thenReturn("not-json");
        Optional<IdempotentSummary> found = service.findSummary("k4");
        assertThat(found).isEmpty();
    }

    @Test
    void findSummaryShouldReturnEmptyWhenNullFromRedis() {
        when(valueOps.get("idem:k5")).thenReturn(null);
        Optional<IdempotentSummary> found = service.findSummary("k5");
        assertThat(found).isEmpty();
    }

    @Test
    void checkAndSetShouldReturnFalseWhenSetIfAbsentReturnsNull() {
        IdempotentSummary summary = new IdempotentSummary(4L, "test", FIXED_TIME);
        when(valueOps.setIfAbsent(eq("idem:k6"), any(String.class), eq(Duration.ofSeconds(60)))).thenReturn(null);
        boolean result = service.checkAndSet("k6", summary);
        assertThat(result).isFalse();
        verify(valueOps, times(1)).setIfAbsent(eq("idem:k6"), any(String.class), eq(Duration.ofSeconds(60)));
    }
}



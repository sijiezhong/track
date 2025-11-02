package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.dto.IdempotentSummary;
import io.github.sijiezhong.track.testsupport.RedisTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
@EnabledIfEnvironmentVariable(named = "ENABLE_REDIS_IT", matches = "true")
// Note: @DirtiesContext removed - RedisTestBase.cleanupRedis() ensures test isolation
public class RedisIdempotencyServiceIntegrationTest extends RedisTestBase {

    @Autowired(required = false)
    private IdempotencyService idempotencyService;

    @Test
    void checkAndSetShouldBeIdempotent() {
        // 如果未配置 RedisIdempotencyService，则跳过
        if (idempotencyService == null) return;
        String key = "idem-it-1";
        IdempotentSummary summary = new IdempotentSummary(1L, "pv", java.time.LocalDateTime.now());
        boolean first = idempotencyService.checkAndSet(key, summary);
        boolean second = idempotencyService.checkAndSet(key, summary);
        Assertions.assertTrue(first);
        Assertions.assertFalse(second);
        Assertions.assertTrue(idempotencyService.findSummary(key).isPresent());
    }
}



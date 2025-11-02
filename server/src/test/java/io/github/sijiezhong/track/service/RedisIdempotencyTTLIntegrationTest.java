package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.dto.IdempotentSummary;
import io.github.sijiezhong.track.testsupport.RedisTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "idempotency.ttl-seconds=1"
})
@EnabledIfEnvironmentVariable(named = "ENABLE_REDIS_IT", matches = "true")
// Note: @DirtiesContext removed - RedisTestBase.cleanupRedis() ensures test isolation
public class RedisIdempotencyTTLIntegrationTest extends RedisTestBase {

    @Autowired(required = false)
    private IdempotencyService idempotencyService;

    @Test
    void keyShouldExpireAfterTTL() throws Exception {
        if (idempotencyService == null) return;
        String key = "idem-ttl-1";
        IdempotentSummary summary = new IdempotentSummary(1L, "pv", java.time.LocalDateTime.now());
        boolean first = idempotencyService.checkAndSet(key, summary);
        boolean second = idempotencyService.checkAndSet(key, summary);
        Assertions.assertTrue(first);
        Assertions.assertFalse(second);
        // 等待 TTL 过期
        Thread.sleep(1500L);
        boolean third = idempotencyService.checkAndSet(key, summary);
        Assertions.assertTrue(third);
    }
}



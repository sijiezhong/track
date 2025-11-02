package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.dto.IdempotentSummary;
import io.github.sijiezhong.track.testsupport.RedisTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@EnabledIfEnvironmentVariable(named = "ENABLE_REDIS_IT", matches = "true")
// Note: @DirtiesContext removed - RedisTestBase.cleanupRedis() ensures test isolation
public class RedisIdempotencyConcurrencyIntegrationTest extends RedisTestBase {

    @Autowired(required = false)
    private IdempotencyService idempotencyService;

    @Test
    void onlyOneWinnerUnderConcurrency() throws Exception {
        if (idempotencyService == null) return;
        String key = "idem-concurrent-1";
        var pool = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return idempotencyService.checkAndSet(key, new IdempotentSummary(1L, "pv", LocalDateTime.now()));
            }));
        }
        start.countDown();
        int wins = 0;
        for (Future<Boolean> f : futures) {
            if (Boolean.TRUE.equals(f.get())) wins++;
        }
        pool.shutdownNow();
        Assertions.assertEquals(1, wins);
    }
}



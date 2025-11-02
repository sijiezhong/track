package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.dto.IdempotentSummary;
import io.github.sijiezhong.track.service.IdempotencyService;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventCollectIdempotencyIntegrationTest extends PostgresTestBase {

    @TestConfiguration
    static class FakeIdemConfig {
        @Bean
        @org.springframework.context.annotation.Primary
        IdempotencyService inMemoryIdem() {
            return new IdempotencyService() {
                private final Map<String, IdempotentSummary> store = new ConcurrentHashMap<>();
                @Override
                public boolean checkAndSet(String key, IdempotentSummary summary) {
                    if (key == null || key.isEmpty()) return true;
                    return store.putIfAbsent(key, summary) == null;
                }
                @Override
                public Optional<IdempotentSummary> findSummary(String key) {
                    return Optional.ofNullable(store.get(key));
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCollectShouldReturn201Then200WithSameIdemKey() throws Exception {
        String key = "idem-get-1";
        mockMvc.perform(get("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .header("Idempotency-Key", key)
                        .param("eventName", "pv")
                        .param("sessionId", "sess-idem-1")
                        .param("tenantId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .header("Idempotency-Key", key)
                        .param("eventName", "pv")
                        .param("sessionId", "sess-idem-1")
                        .param("tenantId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void postCollectShouldReplay200WhenSummaryExists() throws Exception {
        String key = "idem-post-1";
        // 先发起一次创建，服务会写入摘要
        mockMvc.perform(post("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventName\":\"signup\",\"sessionId\":\"sess-idem-2\",\"tenantId\":1}"))
                .andExpect(status().isCreated());

        // 再次提交相同键，POST 分支会先 findSummary 命中，直接 200
        mockMvc.perform(post("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventName\":\"signup\",\"sessionId\":\"sess-idem-2\",\"tenantId\":1}"))
                .andExpect(status().isOk());
    }
}



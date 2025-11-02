package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Comparator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventFieldModelIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void shouldPersistStructuredFieldsFromHeadersAndUAParsing() throws Exception {
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36";
        String referer = "https://example.com/page";
        String xff = "203.0.113.9";

        String body = "{\n" +
                "  \"eventName\": \"page_view\",\n" +
                "  \"sessionId\": \"sess-evt-1\",\n" +
                "  \"tenantId\": 1\n" +
                "}";

        mockMvc.perform(post("/api/v1/events/collect")
                        .header("User-Agent", ua)
                        .header("Referer", referer)
                        .header("X-Forwarded-For", xff)
                        .header("X-Tenant-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        Event saved = eventRepository.findAll().stream()
                .max(Comparator.comparing(Event::getId))
                .orElseThrow();

        // 结构化字段应被持久化
        Assertions.assertNotNull(saved.getUa());
        Assertions.assertNotNull(saved.getReferrer());
        Assertions.assertNotNull(saved.getIp());
        Assertions.assertTrue(saved.getBrowser() != null && saved.getBrowser().contains("Chrome"));
        Assertions.assertTrue(saved.getOs() != null && saved.getOs().contains("Mac"));
        Assertions.assertTrue(saved.getDevice() != null && saved.getDevice().equalsIgnoreCase("Desktop"));
    }
}



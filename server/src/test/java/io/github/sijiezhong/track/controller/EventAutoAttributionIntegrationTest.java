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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventAutoAttributionIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventRepository eventRepository;

    @Test
    void uaAndIpShouldPopulateStructuredFields() throws Exception {
        String body = "{\n" +
                "  \"eventName\": \"pv\",\n" +
                "  \"sessionId\": \"sess-ua-1\",\n" +
                "  \"tenantId\": 1\n" +
                "}";
        mockMvc.perform(post("/api/v1/events/collect")
                        .header("X-Tenant-Id", 1)
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1")
                        .header("Referer", "https://example.com/product/1")
                        .header("X-Forwarded-For", "198.51.100.23, 70.41.3.18")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        List<Event> all = eventRepository.findAll();
        Assertions.assertFalse(all.isEmpty());
        Event e = all.get(all.size() - 1);
        Assertions.assertEquals("198.51.100.23", e.getIp());
        // 由于解析为最小实现：设备Mobile，OS iOS，浏览器Safari
        Assertions.assertEquals("Mobile", e.getDevice());
        Assertions.assertEquals("iOS", e.getOs());
        Assertions.assertEquals("Safari", e.getBrowser());
    }
}



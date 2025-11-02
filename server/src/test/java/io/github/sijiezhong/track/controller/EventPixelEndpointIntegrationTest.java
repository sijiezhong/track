package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Comparator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventPixelEndpointIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventRepository eventRepository;

    @Test
    void pixelGifShouldReturn1x1AndPersistEvent() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/pixel.gif")
                        .header("X-Tenant-Id", 1)
                        .param("eventName", "pixel_view")
                        .param("sessionId", "sess-px-1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/gif"))
                .andReturn();

        byte[] body = res.getResponse().getContentAsByteArray();
        // 固定1x1 GIF 像素应为 43字节左右（经典透明像素）
        Assertions.assertTrue(body.length > 0 && body.length < 100);

        Event saved = eventRepository.findAll().stream()
                .max(Comparator.comparing(Event::getId))
                .orElseThrow();
        Assertions.assertEquals("pixel_view", saved.getEventName());
        Assertions.assertEquals(1, saved.getTenantId());
    }
}



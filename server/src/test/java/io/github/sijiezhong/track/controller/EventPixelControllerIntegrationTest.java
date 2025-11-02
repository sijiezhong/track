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
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventPixelControllerIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Test
    void pixelGifShouldReturnImageAndPersistEvent() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/pixel.gif")
                        .header("X-Tenant-Id", 1)
                        .param("eventName", "pixel_view")
                        .param("sessionId", "sess-pixel-1")
                        .param("userId", "42"))
                .andExpect(status().isOk())
                .andReturn();

        String ct = res.getResponse().getContentType();
        byte[] body = res.getResponse().getContentAsByteArray();
        Assertions.assertNotNull(ct);
        Assertions.assertTrue(ct.contains("image/gif") || ct.contains(MediaType.IMAGE_GIF_VALUE));
        Assertions.assertTrue(body.length > 0);

        List<Event> all = eventRepository.findAll();
        Assertions.assertFalse(all.isEmpty());
        Event e = all.get(all.size() - 1);
        Assertions.assertEquals("pixel_view", e.getEventName());
        Assertions.assertEquals(1, e.getTenantId());
    }
}



package io.github.sijiezhong.track.controller;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.repository.SessionRepository;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventParquetExportIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SessionRepository sessionRepository;

    @BeforeEach
    void setupData() {
        eventRepository.deleteAll();
        sessionRepository.deleteAll();

        Session s1 = new Session(); s1.setSessionId("sess-exp-1"); s1.setTenantId(1); s1 = sessionRepository.save(s1);
        Session s2 = new Session(); s2.setSessionId("sess-exp-2"); s2.setTenantId(2); s2 = sessionRepository.save(s2);
        LocalDateTime now = LocalDateTime.now().withNano(0);

        eventRepository.save(buildEvent("page_view", 1, s1.getId(), now));
        eventRepository.save(buildEvent("purchase", 2, s2.getId(), now)); // 另一租户，导出时应过滤
    }

    private Event buildEvent(String name, int tenantId, Long sessionPk, LocalDateTime time) {
        Event e = new Event();
        e.setEventName(name);
        e.setTenantId(tenantId);
        e.setSessionId(sessionPk);
        e.setEventTime(time);
        e.setProperties("{}");
        return e;
    }

    @Test
    void exportParquetShouldReturnOctetStreamWithContent() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/events/export.parquet")
                        .header("X-Tenant-Id", 1)
                        .accept(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(status().isOk())
                .andReturn();
        byte[] body = res.getResponse().getContentAsByteArray();
        String contentType = res.getResponse().getContentType();
        Assertions.assertNotNull(contentType);
        Assertions.assertTrue(contentType.contains("application/octet-stream"));
        Assertions.assertTrue(body.length > 0);
    }
}



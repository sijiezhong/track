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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventExportSelectFieldsIntegrationTest extends PostgresTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private SessionRepository sessionRepository;

    @BeforeEach
    void setup() {
        eventRepository.deleteAll();
        sessionRepository.deleteAll();
        Session s1 = new Session();
        s1.setSessionId("sess-exp-sel-1");
        s1.setTenantId(1);
        s1 = sessionRepository.save(s1);
        Session s2 = new Session();
        s2.setSessionId("sess-exp-sel-2");
        s2.setTenantId(1);
        s2 = sessionRepository.save(s2);
        Session s3 = new Session();
        s3.setSessionId("sess-exp-sel-3");
        s3.setTenantId(2);
        s3 = sessionRepository.save(s3);

        eventRepository.save(build("pv", 1, s1.getId()));
        eventRepository.save(build("click", 1, s2.getId()));
        eventRepository.save(build("pv", 2, s3.getId())); // other tenant
    }

    private Event build(String name, int tenant, Long sessionId) {
        Event e = new Event();
        e.setEventName(name);
        e.setTenantId(tenant);
        e.setSessionId(sessionId);
        e.setEventTime(LocalDateTime.now());
        e.setProperties("{}");
        return e;
    }

    @Test
    void exportWithFieldsAndFilter() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/events/export.csv")
                .header("X-Tenant-Id", 1)
                .param("fields", "id,eventName,tenantId")
                .param("eventName", "pv")
                .accept("text/csv"))
                .andExpect(status().isOk())
                .andReturn();
        String csv = res.getResponse().getContentAsString();
        String[] lines = csv.split("\n");
        Assertions.assertTrue(lines[0].equals("id,eventName,tenantId"));
        // only tenant=1 and eventName=pv entries should appear
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank())
                continue;
            Assertions.assertTrue(lines[i].contains(",pv,"));
            Assertions.assertTrue(lines[i].endsWith(",1"));
        }
    }
}

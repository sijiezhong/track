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
public class EventPathAnalyticsIntegrationTest extends PostgresTestBase {

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
        Session s1 = new Session(); s1.setSessionId("sess-path-1"); s1.setTenantId(1); s1 = sessionRepository.save(s1);
        Session s2 = new Session(); s2.setSessionId("sess-path-2"); s2.setTenantId(1); s2 = sessionRepository.save(s2);
        Session s3 = new Session(); s3.setSessionId("sess-path-3"); s3.setTenantId(2); s3 = sessionRepository.save(s3);

        LocalDateTime base = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        // s1: A -> B -> C
        eventRepository.save(event("A", 1, s1.getId(), base.plusMinutes(1)));
        eventRepository.save(event("B", 1, s1.getId(), base.plusMinutes(2)));
        eventRepository.save(event("C", 1, s1.getId(), base.plusMinutes(3)));
        // s2: A -> B
        eventRepository.save(event("A", 1, s2.getId(), base.plusMinutes(1)));
        eventRepository.save(event("B", 1, s2.getId(), base.plusMinutes(2)));
        // other tenant noise
        eventRepository.save(event("A", 2, s3.getId(), base.plusMinutes(1)));
        eventRepository.save(event("Z", 2, s3.getId(), base.plusMinutes(2)));
    }

    private Event event(String name, int tenant, Long sessionPk, LocalDateTime t) {
        Event e = new Event();
        e.setEventName(name);
        e.setTenantId(tenant);
        e.setSessionId(sessionPk);
        e.setEventTime(t);
        e.setProperties("{}");
        return e;
    }

    @Test
    void pathEdgesShouldAggregateBigramsWithinSessions() throws Exception {
        String start = LocalDateTime.now().minusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toString();
        String end = LocalDateTime.now().plusDays(1).withHour(23).withMinute(59).withSecond(59).withNano(0).toString();
        MvcResult res = mockMvc.perform(get("/api/v1/events/path")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ANALYST")
                        .param("startTime", start)
                        .param("endTime", end))
                .andExpect(status().isOk())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        // 应包含 A->B (2) 与 B->C (1)，不应包含噪声 A->Z
        Assertions.assertTrue(json.contains("\"from\":\"A\""));
        Assertions.assertTrue(json.contains("\"to\":\"B\""));
        Assertions.assertTrue(json.contains("\"count\":2"));
        Assertions.assertTrue(json.contains("\"from\":\"B\""));
        Assertions.assertTrue(json.contains("\"to\":\"C\""));
        Assertions.assertTrue(json.contains("\"count\":1"));
        Assertions.assertFalse(json.contains("\"to\":\"Z\""));
    }
}



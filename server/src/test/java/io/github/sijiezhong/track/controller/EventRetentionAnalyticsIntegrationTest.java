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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class EventRetentionAnalyticsIntegrationTest extends PostgresTestBase {

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
        // cohort: day0 注册两位用户，day1 其中一位回访
        LocalDate day0 = LocalDate.now().minusDays(1);
        LocalDate day1 = day0.plusDays(1);
        LocalDateTime d0t = day0.atTime(10, 0);
        LocalDateTime d1t = day1.atTime(11, 0);

        Session s1 = new Session(); s1.setSessionId("sess-ret-1"); s1.setTenantId(1); s1 = sessionRepository.save(s1);
        Session s2 = new Session(); s2.setSessionId("sess-ret-2"); s2.setTenantId(1); s2 = sessionRepository.save(s2);

        eventRepository.save(evt("register", 1, 101, s1.getId(), d0t));
        eventRepository.save(evt("register", 1, 102, s2.getId(), d0t));
        // day1 retained: user 101 登录
        eventRepository.save(evt("login", 1, 101, s1.getId(), d1t));
    }

    private Event evt(String name, int tenant, Integer userId, Long sessionPk, LocalDateTime t) {
        Event e = new Event();
        e.setEventName(name);
        e.setTenantId(tenant);
        e.setUserId(userId);
        e.setSessionId(sessionPk);
        e.setEventTime(t);
        e.setProperties("{}");
        return e;
    }

    @Test
    void dailyRetentionShouldReturnCohortAndRetainedCounts() throws Exception {
        String start = LocalDate.now().minusDays(2).atStartOfDay().toString();
        String end = LocalDate.now().plusDays(1).atTime(23,59,59).toString();
        MvcResult res = mockMvc.perform(get("/api/v1/events/retention")
                        .header("X-Tenant-Id", 1)
                        .header("Authorization", "Bearer role:ANALYST")
                        .param("cohortEvent", "register")
                        .param("returnEvent", "login")
                        .param("day", "1")
                        .param("startTime", start)
                        .param("endTime", end))
                .andExpect(status().isOk())
                .andReturn();
        String json = res.getResponse().getContentAsString();
        // cohort 2, retained 1, rate 0.5
        Assertions.assertTrue(json.contains("\"cohort\":2"));
        Assertions.assertTrue(json.contains("\"retained\":1"));
        Assertions.assertTrue(json.contains("0.5"));
    }
}



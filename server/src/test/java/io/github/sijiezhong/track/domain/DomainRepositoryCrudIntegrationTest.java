package io.github.sijiezhong.track.domain;

import io.github.sijiezhong.track.repository.*;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS)
public class DomainRepositoryCrudIntegrationTest extends PostgresTestBase {

    @Autowired ApplicationRepository applicationRepository;
    @Autowired UserRepository userRepository;
    @Autowired SessionRepository sessionRepository;
    @Autowired EventRepository eventRepository;
    @Autowired WebhookSubscriptionRepository webhookSubscriptionRepository;
    @Autowired AuditLogRepository auditLogRepository;

    @Test
    void applicationCrudAndPreUpdate() {
        Application app = new Application();
        app.setAppKey("k-" + System.nanoTime());
        app.setAppName("Demo App");
        app.setOwnerId(100);
        app.setTenantId(1);
        var saved = applicationRepository.save(app);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();

        LocalDateTime firstUpdate = saved.getUpdateTime();
        saved.setAppName("Renamed");
        var updated = applicationRepository.save(saved);
        assertThat(updated.getUpdateTime()).isAfterOrEqualTo(firstUpdate);
    }

    @Test
    void applicationPrePersistShouldSetCreateTimeWhenNull() {
        Application app = new Application();
        app.setAppKey("k-null-create" + System.nanoTime());
        app.setAppName("Test App");
        app.setTenantId(1);
        app.setCreateTime(null); // explicitly set to null to test PrePersist branch
        var saved = applicationRepository.save(app);
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    @Test
    void applicationPrePersistShouldPreserveCreateTimeWhenSet() {
        LocalDateTime presetTime = LocalDateTime.of(2023, 6, 1, 10, 0);
        Application app = new Application();
        app.setAppKey("k-preset-create" + System.nanoTime());
        app.setAppName("Preset App");
        app.setTenantId(1);
        app.setCreateTime(presetTime);
        var saved = applicationRepository.save(app);
        assertThat(saved.getCreateTime()).isEqualTo(presetTime);
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    @Test
    void userCrudAndPreUpdate() {
        User u = new User();
        u.setUsername("u" + System.nanoTime());
        u.setPassword("p");
        u.setTenantId(1);
        var saved = userRepository.save(u);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();

        LocalDateTime upd0 = saved.getUpdateTime();
        saved.setRealName("Real");
        var updated = userRepository.save(saved);
        assertThat(updated.getUpdateTime()).isAfterOrEqualTo(upd0);
    }

    @Test
    void userPrePersistShouldSetCreateTimeWhenNull() {
        User u = new User();
        u.setUsername("u-null-create" + System.nanoTime());
        u.setPassword("p");
        u.setTenantId(1);
        u.setCreateTime(null); // explicitly set to null to test PrePersist branch
        var saved = userRepository.save(u);
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    @Test
    void userPrePersistShouldPreserveCreateTimeWhenSet() {
        LocalDateTime presetTime = LocalDateTime.of(2023, 1, 1, 12, 0);
        User u = new User();
        u.setUsername("u-preset-create" + System.nanoTime());
        u.setPassword("p");
        u.setTenantId(1);
        u.setCreateTime(presetTime); // set before save
        var saved = userRepository.save(u);
        assertThat(saved.getCreateTime()).isEqualTo(presetTime);
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    @Test
    void sessionCrudAndPreUpdate() {
        Session s = new Session();
        s.setSessionId("sid-" + System.nanoTime());
        s.setTenantId(1);
        var saved = sessionRepository.save(s);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStartTime()).isNotNull();
        assertThat(saved.getEndTime()).isNotNull();
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();

        LocalDateTime upd0 = saved.getUpdateTime();
        saved.setUserAgent("UA");
        var updated = sessionRepository.save(saved);
        assertThat(updated.getUpdateTime()).isAfterOrEqualTo(upd0);
    }

    @Test
    void sessionPrePersistShouldSetCreateTimeWhenNull() {
        Session s = new Session();
        s.setSessionId("sid-null-create" + System.nanoTime());
        s.setTenantId(1);
        s.setCreateTime(null); // explicitly set to null to test PrePersist branch
        var saved = sessionRepository.save(s);
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    @Test
    void sessionPrePersistShouldPreserveCreateTimeWhenSet() {
        LocalDateTime presetTime = LocalDateTime.of(2023, 3, 15, 14, 30);
        Session s = new Session();
        s.setSessionId("sid-preset-create" + System.nanoTime());
        s.setTenantId(1);
        s.setCreateTime(presetTime);
        var saved = sessionRepository.save(s);
        assertThat(saved.getCreateTime()).isEqualTo(presetTime);
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    @Test
    void sessionPrePersistShouldSetStartTimeWhenNull() {
        Session s = new Session();
        s.setSessionId("sid-null-start" + System.nanoTime());
        s.setTenantId(1);
        s.setStartTime(null); // explicitly set to null to test PrePersist branch
        var saved = sessionRepository.save(s);
        assertThat(saved.getStartTime()).isNotNull();
        assertThat(saved.getEndTime()).isNotNull();
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    @Test
    void sessionPrePersistShouldSetEndTimeWhenNull() {
        Session s = new Session();
        s.setSessionId("sid-null-end" + System.nanoTime());
        s.setTenantId(1);
        s.setEndTime(null); // explicitly set to null to test PrePersist branch
        var saved = sessionRepository.save(s);
        assertThat(saved.getStartTime()).isNotNull();
        assertThat(saved.getEndTime()).isNotNull();
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    @Test
    void sessionPrePersistShouldPreserveStartTimeAndEndTimeWhenSet() {
        LocalDateTime presetStartTime = LocalDateTime.of(2023, 4, 1, 8, 0);
        LocalDateTime presetEndTime = LocalDateTime.of(2023, 4, 1, 18, 0);
        Session s = new Session();
        s.setSessionId("sid-preset-times" + System.nanoTime());
        s.setTenantId(1);
        s.setStartTime(presetStartTime);
        s.setEndTime(presetEndTime);
        var saved = sessionRepository.save(s);
        assertThat(saved.getStartTime()).isEqualTo(presetStartTime);
        assertThat(saved.getEndTime()).isEqualTo(presetEndTime);
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    @Test
    void eventCrudAndPreUpdate() {
        // ensure session exists to satisfy FK path used in application code
        Session s = new Session();
        s.setSessionId("sid-evt-" + System.nanoTime());
        s.setTenantId(1);
        sessionRepository.save(s);

        Event e = new Event();
        e.setEventName("pv");
        e.setSessionId(s.getId());
        e.setTenantId(1);
        e.setProperties("{}");
        var saved = eventRepository.save(e);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEventTime()).isNotNull();
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();

        LocalDateTime upd0 = saved.getUpdateTime();
        saved.setIp("1.1.1.1");
        var updated = eventRepository.save(saved);
        assertThat(updated.getUpdateTime()).isAfterOrEqualTo(upd0);
    }

    @Test
    void eventPrePersistShouldSetCreateTimeWhenNull() {
        Session s = new Session();
        s.setSessionId("sid-evt-null-create" + System.nanoTime());
        s.setTenantId(1);
        sessionRepository.save(s);

        Event e = new Event();
        e.setEventName("pv");
        e.setSessionId(s.getId());
        e.setTenantId(1);
        e.setCreateTime(null); // explicitly set to null to test PrePersist branch
        var saved = eventRepository.save(e);
        assertThat(saved.getCreateTime()).isNotNull();
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    @Test
    void eventPrePersistShouldSetEventTimeWhenNull() {
        Session s = new Session();
        s.setSessionId("sid-evt-null-eventtime" + System.nanoTime());
        s.setTenantId(1);
        sessionRepository.save(s);

        Event e = new Event();
        e.setEventName("pv");
        e.setSessionId(s.getId());
        e.setTenantId(1);
        e.setEventTime(null); // explicitly set to null to test PrePersist branch
        var saved = eventRepository.save(e);
        assertThat(saved.getEventTime()).isNotNull();
        assertThat(saved.getCreateTime()).isNotNull();
    }

    @Test
    void eventPrePersistShouldPreserveCreateTimeAndEventTimeWhenSet() {
        Session s = new Session();
        s.setSessionId("sid-evt-preset" + System.nanoTime());
        s.setTenantId(1);
        sessionRepository.save(s);

        LocalDateTime presetCreateTime = LocalDateTime.of(2023, 5, 10, 9, 0);
        LocalDateTime presetEventTime = LocalDateTime.of(2023, 5, 10, 9, 5);
        Event e = new Event();
        e.setEventName("pv");
        e.setSessionId(s.getId());
        e.setTenantId(1);
        e.setCreateTime(presetCreateTime);
        e.setEventTime(presetEventTime);
        var saved = eventRepository.save(e);
        assertThat(saved.getCreateTime()).isEqualTo(presetCreateTime);
        assertThat(saved.getEventTime()).isEqualTo(presetEventTime);
        assertThat(saved.getUpdateTime()).isNotNull();
    }

    @Test
    void webhookAndAuditCrud() {
        WebhookSubscription ws = new WebhookSubscription();
        ws.setTenantId(1);
        ws.setUrl("https://example.org/hook");
        ws.setSecret("sec");
        ws.setEnabled(true);
        var wsSaved = webhookSubscriptionRepository.save(ws);
        assertThat(wsSaved.getId()).isNotNull();

        AuditLog al = new AuditLog();
        al.setTenantId(1);
        al.setUsername("tester");
        al.setMethod("POST");
        al.setPath("/p");
        al.setPayload("{}");
        al.setCreateTime(LocalDateTime.now());
        var alSaved = auditLogRepository.save(al);
        assertThat(alSaved.getId()).isNotNull();
    }
}



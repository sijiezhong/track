package io.github.sijiezhong.track.testsupport;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;
import org.junit.jupiter.api.Test;

import static io.github.sijiezhong.track.testsupport.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that test builders and fixtures work correctly.
 * This ensures our test infrastructure is solid before we use it in other tests.
 */
class TestBuildersTest {

    @Test
    void eventTestBuilder_shouldCreateEventWithAllFields() {
        Event event = EventTestBuilder.create()
                .withEventName(EVENT_PAGE_VIEW)
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(DEFAULT_USER_ID)
                .withProperties(PROPERTIES_WITH_URL)
                .withUa(DEFAULT_UA)
                .withReferrer(DEFAULT_REFERRER)
                .withIp(DEFAULT_IP)
                .withDevice(DEVICE_DESKTOP)
                .withOs(OS_MAC)
                .withBrowser(BROWSER_CHROME)
                .build();

        assertThat(event.getEventName()).isEqualTo(EVENT_PAGE_VIEW);
        assertThat(event.getTenantId()).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(event.getUserId()).isEqualTo(DEFAULT_USER_ID);
        assertThat(event.getProperties()).isEqualTo(PROPERTIES_WITH_URL);
        assertThat(event.getUa()).isEqualTo(DEFAULT_UA);
    }

    @Test
    void sessionTestBuilder_shouldCreateSessionWithFields() {
        Session session = SessionTestBuilder.create()
                .withSessionId(DEFAULT_SESSION_ID)
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(DEFAULT_USER_ID)
                .withUserAgent(DEFAULT_UA)
                .withIp(DEFAULT_IP)
                .build();

        assertThat(session.getSessionId()).isEqualTo(DEFAULT_SESSION_ID);
        assertThat(session.getTenantId()).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(session.getUserId()).isEqualTo(DEFAULT_USER_ID);
        assertThat(session.getUserAgent()).isEqualTo(DEFAULT_UA);
        assertThat(session.getIp()).isEqualTo(DEFAULT_IP);
    }

    @Test
    void eventTestBuilder_shouldWorkWithSessionObject() {
        Session session = SessionTestBuilder.create()
                .withSessionId("test-sess")
                .build();
        session.setId(100L); // Simulate saved entity

        Event event = EventTestBuilder.create()
                .withEventName(EVENT_CLICK)
                .withSession(session)
                .build();

        assertThat(event.getSessionId()).isEqualTo(100L);
    }

    @Test
    void testFixtures_shouldProvideCommonScenarios() {
        Session session = TestFixtures.createSessionForTenant(1);
        assertThat(session.getTenantId()).isEqualTo(1);

        Event event = TestFixtures.createBasicEvent();
        assertThat(event.getEventName()).isEqualTo(EVENT_PAGE_VIEW);

        Event clickEvent = TestFixtures.createClickEvent();
        assertThat(clickEvent.getEventName()).isEqualTo(EVENT_CLICK);
    }

    @Test
    void testConstants_shouldProvideConsistentValues() {
        assertThat(DEFAULT_TENANT_ID).isEqualTo(1);
        assertThat(DEFAULT_USER_ID).isEqualTo(10);
        assertThat(EVENT_PAGE_VIEW).isEqualTo("page_view");
        assertThat(PROPERTIES_WITH_URL).contains("\"url\"");
    }
}


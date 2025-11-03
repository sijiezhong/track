package io.github.sijiezhong.track.repository;

import io.github.sijiezhong.track.domain.Session;
import io.github.sijiezhong.track.testsupport.PostgresTestBase;
import io.github.sijiezhong.track.testsupport.SessionTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static io.github.sijiezhong.track.testsupport.TestConstants.DEFAULT_TENANT_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SessionRepository custom query methods.
 * 
 * Coverage includes:
 * - findBySessionId with existing session
 * - findBySessionId with non-existing session
 * - Edge cases (null sessionId, empty sessionId)
 */
public class SessionRepositoryTest extends PostgresTestBase {

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    @DisplayName("Should find session by sessionId when session exists")
    void should_FindBySessionId_When_SessionExists() {
        // Given: Create and save a session
        Session session = SessionTestBuilder.create()
                .withSessionId("test-session-1")
                .withTenantId(DEFAULT_TENANT_ID)
                .withUserId(100)
                .build();
        Session saved = sessionRepository.save(session);

        // When: Find by sessionId
        Optional<Session> found = sessionRepository.findBySessionId("test-session-1");

        // Then: Should find the session
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getSessionId()).isEqualTo("test-session-1");
        assertThat(found.get().getTenantId()).isEqualTo(DEFAULT_TENANT_ID);
        assertThat(found.get().getUserId()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should return empty when sessionId not found")
    void should_ReturnEmpty_When_SessionIdNotFound() {
        // Given: No session with this sessionId exists (clean database from @BeforeEach)

        // When: Find by non-existing sessionId
        Optional<Session> found = sessionRepository.findBySessionId("non-existing-session");

        // Then: Should return empty
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should handle null sessionId gracefully")
    void should_HandleNullSessionId_Gracefully() {
        // When: Find by null sessionId
        Optional<Session> found = sessionRepository.findBySessionId(null);

        // Then: Should return empty (null lookup in database returns no results)
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find correct session when multiple sessions exist")
    void should_FindCorrectSession_When_MultipleSessionsExist() {
        // Given: Create multiple sessions with different sessionIds
        Session session1 = SessionTestBuilder.create()
                .withSessionId("session-1")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        sessionRepository.save(session1);

        Session session2 = SessionTestBuilder.create()
                .withSessionId("session-2")
                .withTenantId(DEFAULT_TENANT_ID)
                .build();
        sessionRepository.save(session2);

        Session session3 = SessionTestBuilder.create()
                .withSessionId("session-3")
                .withTenantId(2)
                .build();
        sessionRepository.save(session3);

        // When: Find by specific sessionId
        Optional<Session> found = sessionRepository.findBySessionId("session-2");

        // Then: Should find the correct session
        assertThat(found).isPresent();
        assertThat(found.get().getSessionId()).isEqualTo("session-2");
        assertThat(found.get().getTenantId()).isEqualTo(DEFAULT_TENANT_ID);
    }
}






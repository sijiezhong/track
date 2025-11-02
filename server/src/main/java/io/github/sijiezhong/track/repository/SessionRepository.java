package io.github.sijiezhong.track.repository;

import io.github.sijiezhong.track.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findBySessionId(String sessionId);

    /**
     * Find session by sessionId with pessimistic write lock.
     * This ensures thread-safe session lookup in concurrent scenarios.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Session s WHERE s.sessionId = :sessionId")
    Optional<Session> findBySessionIdWithLock(@Param("sessionId") String sessionId);

    /**
     * Find session by sessionId with pessimistic write lock and NOWAIT.
     * Returns empty if lock cannot be acquired immediately.
     * This allows non-blocking lock attempts in high concurrency scenarios.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(value = "SELECT * FROM session WHERE session_id = :sessionId FOR UPDATE NOWAIT", nativeQuery = true)
    Optional<Session> findBySessionIdWithLockNowait(@Param("sessionId") String sessionId);
}

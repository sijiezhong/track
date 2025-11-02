package io.github.sijiezhong.track.service;

import io.github.sijiezhong.track.domain.Event;
import io.github.sijiezhong.track.domain.Session;
import io.github.sijiezhong.track.dto.EventCollectRequest;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.repository.SessionRepository;
import io.github.sijiezhong.track.stream.EventStreamBroadcaster;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件服务：负责上报落库（TDD 最小实现）
 */
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final SessionRepository sessionRepository;
    private final EventStreamBroadcaster broadcaster;
    private final WebhookService webhookService;
    private final Counter eventsCreatedCounter;

    @PersistenceContext
    private EntityManager entityManager;

    // Self-injection for proper @Transactional proxy behavior
    // Use @Lazy to avoid circular dependency
    @Autowired
    @Lazy
    private EventService self;

    // Per-sessionId locks for concurrent session creation
    private static final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();

    public EventService(EventRepository eventRepository, SessionRepository sessionRepository,
            EventStreamBroadcaster broadcaster, WebhookService webhookService, MeterRegistry meterRegistry) {
        this.eventRepository = eventRepository;
        this.sessionRepository = sessionRepository;
        this.broadcaster = broadcaster;
        this.webhookService = webhookService;
        this.eventsCreatedCounter = meterRegistry != null
                ? Counter.builder("events_created_total").description("Total events created").register(meterRegistry)
                : null;
    }

    /**
     * 将请求转换为事件实体并保存
     */
    @org.springframework.transaction.annotation.Transactional
    public Event save(EventCollectRequest req) {
        Long sessionPk = null;
        if (req.getSessionId() != null && !req.getSessionId().isBlank()) {
            // Use REQUIRES_NEW to isolate session creation from main transaction
            // This ensures that if session creation fails, it doesn't affect the main
            // transaction
            // Wrap in try-catch to ensure any exceptions from session creation don't affect
            // main transaction
            Session sess;
            try {
                sess = self.findOrCreateSessionInNewTransactionIsolated(req.getSessionId(), req.getUserId(),
                        req.getTenantId());
            } catch (RuntimeException e) {
                // If session creation/retrieval fails, try to find it one more time in a fresh
                // transaction
                // This is a last resort to handle edge cases
                sess = self.findSessionInNewTransaction(req.getSessionId());
                if (sess == null) {
                    // If still not found, rethrow the exception
                    throw new RuntimeException("Failed to find or create session: " + req.getSessionId(), e);
                }
            }
            // 匿名到实名合并：若会话当前为匿名且本次事件携带userId，则回填并保存
            if (req.getUserId() != null && sess.getUserId() == null) {
                sess.setUserId(req.getUserId());
                // Update in a new transaction to avoid any issues
                updateSessionInNewTransaction(sess);
            }
            sessionPk = sess.getId();
        }

        Event e = new Event();
        e.setEventName(req.getEventName());
        e.setUserId(req.getUserId());
        e.setTenantId(req.getTenantId());
        e.setSessionId(sessionPk);
        e.setProperties(req.getProperties() == null ? null : req.getProperties().toString());
        // 结构化字段
        e.setUa(req.getUa());
        e.setReferrer(req.getReferrer());
        e.setIp(req.getIp());
        e.setDevice(req.getDevice());
        e.setOs(req.getOs());
        e.setBrowser(req.getBrowser());
        e.setChannel(req.getChannel());
        e.setAnonymousId(req.getAnonymousId());
        e.setEventTime(LocalDateTime.now());
        e.setCreateTime(LocalDateTime.now());
        e.setUpdateTime(LocalDateTime.now());
        Event saved = eventRepository.save(e);
        if (eventsCreatedCounter != null) {
            eventsCreatedCounter.increment();
        }
        // 推送SSE
        broadcaster.broadcastEvent(saved.getTenantId(), saved);
        // 触发Webhook（若开启）
        if (webhookService != null) {
            webhookService.onEvent(saved);
        }
        return saved;
    }

    /**
     * Find or create a session in a thread-safe manner within the current
     * transaction.
     * Uses application-level synchronization per sessionId to prevent concurrent
     * creation.
     * Creates session directly in current transaction with retry on duplicate key
     * exception.
     */
    private Session findOrCreateSessionInTransaction(String sessionId, Integer userId, Integer tenantId) {
        // Get or create a lock object for this specific sessionId
        // Use synchronized to ensure thread-safe lock creation
        Object lock = sessionLocks.get(sessionId);
        if (lock == null) {
            synchronized (sessionLocks) {
                // Double-check pattern
                lock = sessionLocks.get(sessionId);
                if (lock == null) {
                    lock = new Object();
                    sessionLocks.put(sessionId, lock);
                }
            }
        }

        synchronized (lock) {
            try {
                // First, try to find existing session with pessimistic lock
                Optional<Session> found = sessionRepository.findBySessionIdWithLock(sessionId);
                if (found.isPresent()) {
                    return found.get();
                }

                // Session doesn't exist, try to create it in a new transaction
                // Using REQUIRES_NEW ensures that if creation fails, the main transaction is
                // not affected
                Session created = createSessionInNewTransactionSafe(sessionId, userId, tenantId);
                if (created != null) {
                    return created;
                }

                // Creation returned null: another thread created it concurrently in a
                // REQUIRES_NEW transaction
                // Retry finding immediately in a fresh transaction (most likely it's already
                // committed)
                // Use exponential backoff with shorter initial delay
                int maxRetries = 10;
                for (int attempt = 0; attempt < maxRetries; attempt++) {
                    // First attempt immediately, then exponential backoff: 10ms, 20ms, 40ms, 80ms,
                    // ...
                    if (attempt > 0) {
                        long delay = 10L * (1L << (attempt - 1)); // 10ms, 20ms, 40ms, 80ms, 160ms, ...
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted while waiting for concurrent session creation", ie);
                        }
                    }

                    // Find in a fresh read-only transaction (should see committed session from
                    // other thread)
                    Session foundInNewTx = findSessionInNewTransaction(sessionId);
                    if (foundInNewTx != null) {
                        return foundInNewTx;
                    }
                }

                throw new RuntimeException("Failed to find session after concurrent creation after " + maxRetries
                        + " retries. This indicates a serious concurrency issue.");
            } finally {
                // Clean up lock if no other thread is waiting
                sessionLocks.remove(sessionId, lock);
            }
        }
    }

    /**
     * Find or create a session in a thread-safe manner.
     * Uses application-level synchronization per sessionId to prevent concurrent
     * creation.
     * Uses REQUIRES_NEW transaction for creation to isolate failures from main
     * transaction.
     * 
     * @deprecated Use findOrCreateSessionInTransaction instead for better
     *             reliability
     */
    @Deprecated
    private Session findOrCreateSession(String sessionId, Integer userId, Integer tenantId) {
        return findOrCreateSessionInTransaction(sessionId, userId, tenantId);
    }

    /**
     * Find or create session in a completely isolated new transaction.
     * This method handles all session operations using REQUIRES_NEW transactions
     * to ensure the main transaction is never affected by session creation issues.
     * Must be public for @Transactional to work (Spring AOP requirement).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Session findOrCreateSessionInNewTransactionIsolated(String sessionId, Integer userId, Integer tenantId) {
        // Get or create a lock object for this specific sessionId
        Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());

        synchronized (lock) {
            try {
                // First, try to find existing session with pessimistic lock
                Optional<Session> found = sessionRepository.findBySessionIdWithLock(sessionId);
                if (found.isPresent()) {
                    return found.get();
                }

                // Session doesn't exist, try to create it
                // Double-check before creating to handle race condition within transaction
                // boundary
                // Another thread might have created it between the previous check and now
                found = sessionRepository.findBySessionIdWithLock(sessionId);
                if (found.isPresent()) {
                    return found.get();
                }

                // Still doesn't exist, create it
                // If creation fails due to concurrent creation, use a fresh REQUIRES_NEW
                // transaction to find it
                try {
                    Session s = new Session();
                    s.setSessionId(sessionId);
                    s.setUserId(userId);
                    s.setTenantId(tenantId);
                    s.setStartTime(LocalDateTime.now());
                    s.setEndTime(LocalDateTime.now());
                    s.setCreateTime(LocalDateTime.now());
                    s.setUpdateTime(LocalDateTime.now());
                    Session saved = sessionRepository.save(s);
                    // Flush immediately to catch duplicate key violation early
                    entityManager.flush();
                    return saved;
                } catch (org.springframework.dao.DataIntegrityViolationException
                        | jakarta.persistence.PersistenceException e) {
                    // Another thread created the session concurrently
                    // This transaction is now aborted, so we need to find it in a fresh
                    // REQUIRES_NEW transaction
                    // Use self-injection to call a fresh REQUIRES_NEW method
                    // Use shorter delays and more retries for better concurrency handling
                    int maxRetries = 20;
                    for (int attempt = 0; attempt < maxRetries; attempt++) {
                        // First attempt immediately, then very short delays
                        if (attempt > 0) {
                            try {
                                // Use exponential backoff: 5ms, 10ms, 20ms, 40ms, ...
                                long delay = attempt == 1 ? 5L : (5L * (1L << (attempt - 1)));
                                Thread.sleep(delay);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException("Interrupted while waiting for concurrent session creation",
                                        ie);
                            }
                        }

                        // Find in a fresh REQUIRES_NEW transaction (should see committed session)
                        Session foundInNewTx = self.findSessionInNewTransaction(sessionId);
                        if (foundInNewTx != null) {
                            return foundInNewTx;
                        }
                    }

                    throw new RuntimeException("Failed to create or find session after " + maxRetries + " retries.", e);
                }
            } finally {
                sessionLocks.remove(sessionId, lock);
            }
        }
    }

    /**
     * Attempt to create session in a nested REQUIRES_NEW transaction.
     * If creation fails due to concurrent creation, the transaction rolls back and
     * returns null.
     * This nested transaction ensures the outer REQUIRES_NEW transaction is not
     * affected.
     * Must be public for @Transactional to work (Spring AOP requirement).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Session attemptCreateSessionInIsolatedTransaction(String sessionId, Integer userId, Integer tenantId) {
        try {
            Session s = new Session();
            s.setSessionId(sessionId);
            s.setUserId(userId);
            s.setTenantId(tenantId);
            s.setStartTime(LocalDateTime.now());
            s.setEndTime(LocalDateTime.now());
            s.setCreateTime(LocalDateTime.now());
            s.setUpdateTime(LocalDateTime.now());
            Session saved = sessionRepository.save(s);
            // Flush immediately to catch duplicate key violation early
            entityManager.flush();
            return saved;
        } catch (org.springframework.dao.DataIntegrityViolationException | jakarta.persistence.PersistenceException e) {
            // Another thread created the session concurrently
            // Let this nested transaction roll back naturally
            // Return null to signal failure
            return null;
        }
    }

    /**
     * Update session in a new transaction.
     * Used to update session userId when converting from anonymous to real user.
     * Must be public for @Transactional to work (Spring AOP requirement).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateSessionInNewTransaction(Session session) {
        sessionRepository.save(session);
    }

    /**
     * Find session in a new transaction.
     * Used when another transaction may have created the session but we need a
     * fresh view.
     * Must be public for @Transactional to work (Spring AOP requirement).
     * Note: Not read-only because we use pessimistic lock (SELECT FOR UPDATE).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Session findSessionInNewTransaction(String sessionId) {
        return sessionRepository.findBySessionIdWithLock(sessionId).orElse(null);
    }

    /**
     * Safely create session in a new transaction with proper error handling.
     * If creation fails due to concurrent creation, returns null to allow caller to
     * retry.
     * Must be public for @Transactional to work (Spring AOP requirement).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Session createSessionInNewTransactionSafe(String sessionId, Integer userId, Integer tenantId) {
        // Try to find again (another thread may have created it between the
        // synchronized check and this transaction)
        Optional<Session> found = sessionRepository.findBySessionIdWithLock(sessionId);
        if (found.isPresent()) {
            return found.get();
        }

        // Create new session
        // Use flush immediately to catch duplicate key exception early
        // and avoid Hibernate persistence context issues
        try {
            Session s = new Session();
            s.setSessionId(sessionId);
            s.setUserId(userId);
            s.setTenantId(tenantId);
            s.setStartTime(LocalDateTime.now());
            s.setEndTime(LocalDateTime.now());
            s.setCreateTime(LocalDateTime.now());
            s.setUpdateTime(LocalDateTime.now());
            Session saved = sessionRepository.save(s);
            // Flush immediately to catch duplicate key violation before any other operation
            entityManager.flush();
            return saved;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Another thread created the session concurrently
            // Clear the persistence context to remove the failed entity
            // This prevents Hibernate from trying to flush it later
            entityManager.clear();
            // PostgreSQL aborts the transaction on error
            // Return null and let caller find it in a fresh transaction
            return null;
        } catch (jakarta.persistence.PersistenceException e) {
            // Also catch JPA-level exceptions (may wrap DataIntegrityViolationException)
            entityManager.clear();
            return null;
        }
    }

    /**
     * Create session in a new transaction to isolate failures.
     * If creation fails due to concurrent creation, returns null to allow retry in
     * a fresh transaction.
     * Must be public for @Transactional to work (Spring AOP requirement).
     * 
     * @deprecated Use createSessionInNewTransactionSafe instead
     */
    @Deprecated
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Session createSessionInNewTransaction(String sessionId, Integer userId, Integer tenantId) {
        return createSessionInNewTransactionSafe(sessionId, userId, tenantId);
    }
}

package io.github.sijiezhong.track.testsupport;

import io.github.sijiezhong.track.repository.ApplicationRepository;
import io.github.sijiezhong.track.repository.AuditLogRepository;
import io.github.sijiezhong.track.repository.EventRepository;
import io.github.sijiezhong.track.repository.SessionRepository;
import io.github.sijiezhong.track.repository.UserRepository;
import io.github.sijiezhong.track.repository.WebhookSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Integration test base class: provides Testcontainers PostgreSQL container.
 * 
 * <p>This base class provides:
 * <ul>
 *   <li>Shared Testcontainers PostgreSQL 15 container (reused across all test classes)</li>
 *   <li>Automatic database cleanup before each test</li>
 *   <li>Repository access for test data setup</li>
 * </ul>
 * 
 * <p>All tests extending this class will have a clean database state before each test method,
 * eliminating the need for manual cleanup and reducing the need for @DirtiesContext.
 * 
 * <p>Performance optimization: Uses SharedPostgresContainer to reuse a single container
 * across all test classes, dramatically reducing container startup overhead.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class PostgresTestBase {

  // Repositories for database cleanup - available to subclasses
  @Autowired
  protected EventRepository eventRepository;
  
  @Autowired
  protected SessionRepository sessionRepository;
  
  @Autowired
  protected ApplicationRepository applicationRepository;
  
  @Autowired
  protected UserRepository userRepository;
  
  @Autowired
  protected AuditLogRepository auditLogRepository;
  
  @Autowired
  protected WebhookSubscriptionRepository webhookSubscriptionRepository;

  @Autowired(required = false)
  protected JdbcTemplate jdbcTemplate;

  @Autowired(required = false)
  protected TransactionTemplate transactionTemplate;
  
  @Autowired(required = false)
  protected javax.sql.DataSource dataSource;

  // 将共享容器的连接信息注入到 Spring 环境
  @DynamicPropertySource
  static void overrideProps(DynamicPropertyRegistry registry) {
    // Get the shared container instance (will be created if not exists)
    PostgreSQLContainer<?> container = SharedPostgresContainer.getInstance();
    
    registry.add("spring.datasource.url", container::getJdbcUrl);
    registry.add("spring.datasource.username", container::getUsername);
    registry.add("spring.datasource.password", container::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    
    // Disable Flyway auto-migration in tests
    // Migrations are executed once by SharedPostgresContainer on first container start
    registry.add("spring.flyway.enabled", () -> "false");
    
    // Optimized HikariCP connection pool configuration for shared container
    // With shared container, connections are stable and can have longer lifetimes
    registry.add("spring.datasource.hikari.maximum-pool-size", () -> "20");
    registry.add("spring.datasource.hikari.minimum-idle", () -> "5");
    // Reduced connection timeout from 30s to 5s for faster failure detection
    registry.add("spring.datasource.hikari.connection-timeout", () -> "5000");
    // Reduced idle-timeout to 1 minute (connections idle for 1 min are closed)
    registry.add("spring.datasource.hikari.idle-timeout", () -> "60000"); // 1 minute
    // Increased max-lifetime to 5 minutes (must be > idle-timeout + 30s per HikariCP requirement)
    // Longer lifetime is safe with shared container as connections remain valid
    registry.add("spring.datasource.hikari.max-lifetime", () -> "300000"); // 5 minutes
    // Allow connection leak detection in tests
    registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000");
    // Force connection validation before use
    registry.add("spring.datasource.hikari.connection-test-query", () -> "SELECT 1");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    registry.add("spring.jpa.show-sql", () -> "true");
    // Disable Flyway auto-migration in all tests - migrations are handled by SharedPostgresContainer
    // This ensures migrations are executed once on container start, not per test class
    registry.add("spring.flyway.enabled", () -> "false");
    // Don't set Redis properties - RedisIdempotencyService is conditionally created
    // only when StringRedisTemplate bean exists. Tests that need Redis should use RedisTestBase.
  }

  /**
   * Cleans up all database tables before each test.
   * This ensures test isolation and eliminates the need for @DirtiesContext in most cases.
   * 
   * <p>Uses TRUNCATE CASCADE for faster cleanup compared to deleteAll().
   * TRUNCATE is much faster as it doesn't log individual row deletions.
   * 
   * <p>Cleanup order matters due to foreign key constraints, but TRUNCATE CASCADE
   * handles this automatically by truncating dependent tables first.
   * 
   * <p>Subclasses can override this method to add custom cleanup logic,
   * but should call super.cleanupDatabase() first.
   */
  @BeforeEach
  void cleanupDatabase() {
    // Get shared container (should already be running)
    PostgreSQLContainer<?> container = SharedPostgresContainer.getInstance();
    
    if (jdbcTemplate != null) {
      // Use TRUNCATE CASCADE for much faster cleanup (10-100x faster than deleteAll)
      // TRUNCATE CASCADE automatically handles foreign key dependencies
      // Order doesn't matter with CASCADE - PostgreSQL handles it automatically
      // Execute within a transaction to ensure atomicity
      // With shared container, connection issues should be minimal, but keep simple retry
      if (transactionTemplate != null) {
        // Simple retry (max 2 attempts) for shared container scenario
        int maxRetries = 2;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
          try {
            transactionTemplate.executeWithoutResult(status -> {
              jdbcTemplate.execute("TRUNCATE TABLE event, session, audit_log, webhook_subscription, application, users RESTART IDENTITY CASCADE");
            });
            // ✅ P0修复：清理后必须验证清理成功
            verifyDatabaseClean();
            break; // Success, exit retry loop
          } catch (org.springframework.transaction.CannotCreateTransactionException e) {
            // ✅ P0修复：不允许静默失败，清理失败必须导致测试失败
            // With shared container, connection errors should be rare
            // Simple fallback to repository deleteAll on last attempt
            if (attempt == maxRetries - 1) {
              System.err.println("CRITICAL: Transaction creation failed, trying fallback cleanup with repositories");
              try {
                if (eventRepository != null) eventRepository.deleteAll();
                if (sessionRepository != null) sessionRepository.deleteAll();
                if (auditLogRepository != null) auditLogRepository.deleteAll();
                if (webhookSubscriptionRepository != null) webhookSubscriptionRepository.deleteAll();
                if (applicationRepository != null) applicationRepository.deleteAll();
                if (userRepository != null) userRepository.deleteAll();
                // ✅ P0修复：验证fallback清理也成功
                verifyDatabaseClean();
              } catch (Exception fallbackException) {
                // ✅ P0修复：Fallback失败必须抛出异常，不允许静默失败
                throw new RuntimeException(
                    "CRITICAL: Database cleanup failed completely. " +
                    "Both TRUNCATE and fallback deleteAll failed. " +
                    "Container running: " + container.isRunning() + ". " +
                    "Tests cannot proceed with dirty database state. " +
                    "Error: " + fallbackException.getMessage(),
                    fallbackException);
              }
            }
            
            // Wait briefly before retry
            try {
              Thread.sleep(100 * (attempt + 1)); // Exponential backoff: 100ms, 200ms
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              throw new RuntimeException("Interrupted during database cleanup retry", ie);
            }
          }
        }
      } else {
        // Fallback if TransactionTemplate is not available
        jdbcTemplate.execute("TRUNCATE TABLE event, session, audit_log, webhook_subscription, application, users RESTART IDENTITY CASCADE");
        // ✅ P0修复：验证清理成功
        verifyDatabaseClean();
      }
    } else {
      // Fallback to deleteAll if JdbcTemplate is not available
      if (eventRepository != null) {
        eventRepository.deleteAll();
      }
      if (sessionRepository != null) {
        sessionRepository.deleteAll();
      }
      if (auditLogRepository != null) {
        auditLogRepository.deleteAll();
      }
      if (webhookSubscriptionRepository != null) {
        webhookSubscriptionRepository.deleteAll();
      }
      if (applicationRepository != null) {
        applicationRepository.deleteAll();
      }
      if (userRepository != null) {
        userRepository.deleteAll();
      }
      // ✅ P0修复：验证清理成功
      verifyDatabaseClean();
    }
  }

  /**
   * ✅ P0修复：验证数据库清理是否成功
   * 如果清理失败，抛出异常导致测试失败
   */
  protected void verifyDatabaseClean() {
    if (jdbcTemplate != null) {
      try {
        // 验证所有表都是空的
        Long eventCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM event", Long.class);
        Long sessionCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM session", Long.class);
        
        if (eventCount != null && eventCount > 0) {
          throw new RuntimeException("CRITICAL: Database cleanup failed - event table still contains " + eventCount + " rows");
        }
        if (sessionCount != null && sessionCount > 0) {
          throw new RuntimeException("CRITICAL: Database cleanup failed - session table still contains " + sessionCount + " rows");
        }
        
        // 可选：验证其他表（如果表存在）
        try {
          Long auditLogCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_log", Long.class);
          if (auditLogCount != null && auditLogCount > 0) {
            System.err.println("WARNING: audit_log table still contains " + auditLogCount + " rows after cleanup");
          }
        } catch (Exception ignored) {
          // 表可能不存在，忽略
        }
      } catch (org.springframework.dao.DataAccessException e) {
        // 如果查询失败，可能是表不存在或连接问题
        // 但这是严重问题，应该抛出异常
        throw new RuntimeException("CRITICAL: Failed to verify database cleanup. Cannot proceed with tests.", e);
      }
    } else if (eventRepository != null && sessionRepository != null) {
      // Fallback验证：使用Repository
      long eventCount = eventRepository.count();
      long sessionCount = sessionRepository.count();
      
      if (eventCount > 0) {
        throw new RuntimeException("CRITICAL: Database cleanup failed - event table still contains " + eventCount + " rows");
      }
      if (sessionCount > 0) {
        throw new RuntimeException("CRITICAL: Database cleanup failed - session table still contains " + sessionCount + " rows");
      }
    }
  }
}

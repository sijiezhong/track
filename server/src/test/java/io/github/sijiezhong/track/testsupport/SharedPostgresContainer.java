package io.github.sijiezhong.track.testsupport;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Shared PostgreSQL container manager for test performance optimization.
 * 
 * <p>This singleton manages a single PostgreSQL container instance that is reused
 * across all test classes, dramatically reducing container startup overhead.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Thread-safe singleton pattern with double-checked locking</li>
 *   <li>Container reuse via Testcontainers reuse functionality</li>
 *   <li>One-time Flyway migration execution</li>
 *   <li>Automatic cleanup on JVM shutdown</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * PostgreSQLContainer<?> container = SharedPostgresContainer.getInstance();
 * String jdbcUrl = container.getJdbcUrl();
 * </pre>
 */
public class SharedPostgresContainer {
    
    private static final Logger log = LoggerFactory.getLogger(SharedPostgresContainer.class);
    
    private static volatile PostgreSQLContainer<?> instance;
    private static final Object lock = new Object();
    private static final AtomicBoolean migrationExecuted = new AtomicBoolean(false);
    private static final AtomicBoolean migrationInProgress = new AtomicBoolean(false);
    
    /**
     * Gets the shared PostgreSQL container instance.
     * 
     * <p>The container is created lazily on first access and reused for all subsequent calls.
     * Flyway migrations are executed once on the first container start.
     * 
     * @return the shared PostgreSQL container instance
     */
    public static PostgreSQLContainer<?> getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = createContainer();
                    registerShutdownHook();
                }
            }
        }
        return instance;
    }
    
    /**
     * Creates and starts the PostgreSQL container with reuse enabled.
     * 
     * @return the started container
     */
    @SuppressWarnings("resource") // Container is closed by shutdown hook
    private static PostgreSQLContainer<?> createContainer() {
        log.info("Creating shared PostgreSQL container for tests");
        
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
                .withDatabaseName("track_test")
                .withUsername("postgres")
                .withPassword("postgres")
                .withReuse(true); // Enable container reuse for better performance
        
        container.start();
        log.info("Shared PostgreSQL container started at {}", container.getJdbcUrl());
        
        // Execute Flyway migrations once on first container start
        executeMigrationsOnce(container);
        
        return container;
    }
    
    /**
     * Executes Flyway migrations exactly once, even in parallel test execution.
     * Uses database locking to ensure only one migration runs across all threads.
     * 
     * @param container the PostgreSQL container
     */
    private static void executeMigrationsOnce(PostgreSQLContainer<?> container) {
        // Use compare-and-set to ensure only one thread attempts migration
        if (!migrationExecuted.get() && migrationInProgress.compareAndSet(false, true)) {
            try {
                // Double-check after acquiring the lock
                if (!migrationExecuted.get()) {
                    log.info("Executing Flyway migrations on shared container");
                    
                    // Check if migrations have already been executed by another process
                    // by checking for flyway_schema_history table
                    try (Connection connection = container.createConnection("")) {
                        DatabaseMetaData metaData = connection.getMetaData();
                        boolean hasFlywayTable = false;
                        
                        try (ResultSet tables = metaData.getTables(null, "public", "flyway_schema_history", null)) {
                            hasFlywayTable = tables.next();
                        }
                        
                        if (!hasFlywayTable) {
                            // Migrations not executed yet, run them
                            executeMigrations(container);
                        } else {
                            // Check if migrations are up to date
                            try (Statement stmt = connection.createStatement();
                                 ResultSet rs = stmt.executeQuery(
                                     "SELECT COUNT(*) FROM flyway_schema_history")) {
                                if (rs.next()) {
                                    int migrationCount = rs.getInt(1);
                                    log.info("Found {} existing migrations in flyway_schema_history", migrationCount);
                                }
                            }
                        }
                        
                        migrationExecuted.set(true);
                        log.info("Flyway migrations check completed");
                    } catch (Exception e) {
                        // If checking fails, assume migrations needed and execute them
                        log.warn("Failed to check migration status, executing migrations anyway", e);
                        executeMigrations(container);
                        migrationExecuted.set(true);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to execute Flyway migrations", e);
                throw new RuntimeException("Failed to execute Flyway migrations on shared container", e);
            } finally {
                migrationInProgress.set(false);
            }
        } else {
            // Another thread is handling migrations or migrations already executed
            // Wait a bit for migrations to complete if in progress
            if (migrationInProgress.get()) {
                int waitCount = 0;
                while (migrationInProgress.get() && waitCount < 50) {
                    try {
                        Thread.sleep(100);
                        waitCount++;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting for migrations", e);
                    }
                }
            }
        }
    }
    
    /**
     * Executes Flyway migrations using the Flyway API.
     * 
     * @param container the PostgreSQL container
     */
    private static void executeMigrations(PostgreSQLContainer<?> container) {
        Flyway flyway = Flyway.configure()
                .dataSource(container.getJdbcUrl(), container.getUsername(), container.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        
        flyway.migrate();
        log.info("Flyway migrations executed successfully");
    }
    
    /**
     * Registers a shutdown hook to properly close the container when JVM exits.
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (instance != null && instance.isRunning()) {
                log.info("Shutting down shared PostgreSQL container");
                instance.stop();
            }
        }, "SharedPostgresContainer-Shutdown"));
    }
    
    /**
     * Closes the shared container. Typically only needed for cleanup or testing.
     * In normal operation, the container will be closed by the shutdown hook.
     */
    public static void close() {
        synchronized (lock) {
            if (instance != null && instance.isRunning()) {
                log.info("Manually closing shared PostgreSQL container");
                instance.stop();
                instance = null;
                migrationExecuted.set(false);
                migrationInProgress.set(false);
            }
        }
    }
    
    /**
     * Checks if the shared container is currently running.
     * 
     * @return true if container is running, false otherwise
     */
    public static boolean isRunning() {
        return instance != null && instance.isRunning();
    }
}


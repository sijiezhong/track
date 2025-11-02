package io.github.sijiezhong.track.testsupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test base class: provides Testcontainers Redis container.
 * 
 * <p>This base class provides:
 * <ul>
 *   <li>Testcontainers Redis 7 container</li>
 *   <li>Automatic Redis cleanup before each test</li>
 *   <li>Redis connection access for test data setup</li>
 * </ul>
 * 
 * <p>All tests extending this class will have a clean Redis state before each test method,
 * eliminating the need for @DirtiesContext for Redis state pollution.
 */
@Testcontainers
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class RedisTestBase {
    @Container
    @SuppressWarnings("resource")
    public static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7").withExposedPorts(6379);

    @Autowired(required = false)
    protected RedisConnectionFactory redisConnectionFactory;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
        registry.add("spring.data.redis.host", () -> REDIS.getHost());
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    /**
     * Cleans up all Redis data before each test.
     * This ensures test isolation and eliminates the need for @DirtiesContext in most cases.
     * 
     * <p>Subclasses can override this method to add custom cleanup logic,
     * but should call super.cleanupRedis() first.
     */
    @BeforeEach
    void cleanupRedis() {
        if (redisConnectionFactory != null) {
            try {
                redisConnectionFactory.getConnection().flushAll();
            } catch (Exception e) {
                // If Redis is not configured or connection fails, ignore
                // This allows tests to run even when Redis is optional
            }
        }
    }
}



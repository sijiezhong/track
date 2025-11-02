package io.github.sijiezhong.track.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "idempotency")
public class IdempotencyProperties {
    // 幂等键过期时间（秒）
    private long ttlSeconds = 24 * 60 * 60;

    public long getTtlSeconds() { return ttlSeconds; }
    public void setTtlSeconds(long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
}

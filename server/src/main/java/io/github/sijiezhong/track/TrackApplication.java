package io.github.sijiezhong.track;

import io.github.sijiezhong.track.config.IdempotencyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({IdempotencyProperties.class})
public class TrackApplication {
    public static void main(String[] args) {
        SpringApplication.run(TrackApplication.class, args);
    }
}


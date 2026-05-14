package com.marketai.backend.config.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            RedisConnection conn = redisTemplate.getRequiredConnectionFactory().getConnection();
            String pong = new String(conn.ping());
            conn.close();
            return Health.up()
                    .withDetail("redis", pong)
                    .withDetail("status", "connected")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("redis", "disconnected")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
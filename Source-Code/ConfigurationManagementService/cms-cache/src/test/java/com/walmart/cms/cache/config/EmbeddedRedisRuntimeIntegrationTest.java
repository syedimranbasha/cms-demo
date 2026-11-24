package com.walmart.cms.cache.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Starts the same embedded Redis configuration used by the local demo application
 * and verifies that the injected Spring Data Redis template can read and write data.
 */
class EmbeddedRedisRuntimeIntegrationTest {

    @Test
    void embeddedRedisStartsAndStringRedisTemplateCanReadAndWrite() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "testRedis", Map.of("cms.cache.embedded-redis.port", port)));
            context.register(CmsCacheConfiguration.class);
            context.refresh();

            RedisConnectionFactory connectionFactory =
                    context.getBean(RedisConnectionFactory.class);
            StringRedisTemplate template = context.getBean(StringRedisTemplate.class);

            assertNotNull(connectionFactory);
            template.opsForValue().set("cms-cache-integration-test", "redis-is-running");
            assertEquals("redis-is-running",
                    template.opsForValue().get("cms-cache-integration-test"));

            try (RedisConnection connection = connectionFactory.getConnection()) {
                assertEquals("PONG", connection.ping());
            }
        }
    }
}

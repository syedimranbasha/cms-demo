package com.walmart.cms.cache.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import redis.embedded.RedisServer;
import java.io.IOException;

/**
 * Starts an embedded Redis process for the local CMS demo and wires Spring Data Redis
 * to that process. The embedded server is started before the connection factory is
 * created and stopped when the Spring application context is closed.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CmsCacheProperties.class)
public class CmsCacheConfiguration {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(
            prefix = "cms.cache.embedded-redis",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public RedisServer embeddedRedisServer(CmsCacheProperties properties) throws IOException {
        CmsCacheProperties.EmbeddedRedis redis = properties.getEmbeddedRedis();
        validate(redis);
        return RedisServer.newRedisServer()
                .port(redis.getPort())
                .setting("maxmemory 128M")
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "cms.cache.embedded-redis",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public RedisConnectionFactory redisConnectionFactory(
            RedisServer embeddedRedisServer,
            CmsCacheProperties properties) {
        CmsCacheProperties.EmbeddedRedis redis = properties.getEmbeddedRedis();
        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(redis.getHost(), redis.getPort());
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "cms.cache.embedded-redis",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        return new StringRedisTemplate(redisConnectionFactory);
    }

    private static void validate(CmsCacheProperties.EmbeddedRedis redis) {
        if (redis.getHost() == null || redis.getHost().isBlank()) {
            throw new IllegalArgumentException("cms.cache.embedded-redis.host must not be blank");
        }
        if (redis.getPort() < 1 || redis.getPort() > 65535) {
            throw new IllegalArgumentException(
                    "cms.cache.embedded-redis.port must be between 1 and 65535");
        }
    }
}

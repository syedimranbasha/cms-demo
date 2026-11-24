package com.walmart.cms.coreapi.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.walmart.cms.cache.service.EffectiveConfigurationCache;
import com.walmart.cms.cache.model.EffectiveConfiguration;
import com.walmart.cms.cache.model.ConfigurationVersionMetadata;
import com.walmart.cms.cache.model.EffectiveConfigurationCacheKey;

import java.time.Duration;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.walmart.cms.security.TestSecurityConfiguration;

@SpringBootTest(classes = CoreApiIntegrationTest.TestApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@Import(TestSecurityConfiguration.class)
class CoreApiIntegrationTest {
    @DynamicPropertySource
    static void redisPort(DynamicPropertyRegistry registry) throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        registry.add("cms.cache.embedded-redis.port", () -> port);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    EffectiveConfigurationCache cache;

    @Test
    void applicationContextStartsWithEmbeddedPostgresAndRedisAndRouteIsRegistered() throws Exception {
        var key = new EffectiveConfigurationCacheKey("org", "app", "PROD", "us-central1", "baseline", "integration");
        var value = new EffectiveConfiguration(Map.of("feature", true),
                new ConfigurationVersionMetadata("NONE", "NONE", "NONE", "NONE"), null);
        cache.put(key, value, Duration.ofMinutes(1));
        assertEquals(value.effectiveConfig(), cache.get(key).orElseThrow().effectiveConfig());
        mvc.perform(get("/api/v1/organizations/00000000-0000-0000-0000-000000000001/applications/00000000-0000-0000-0000-000000000002/config")
                        .param("environment", "PROD").param("region", "us-central1").header("X-Instance-Id", "instance-1"))
                .andExpect(status().isNotFound());
    }

    @SpringBootApplication(scanBasePackages = "com.walmart.cms")
    @EntityScan("com.walmart.cms.dal.entity")
    @EnableJpaRepositories("com.walmart.cms.dal.repository")
    static class TestApplication {
    }
}

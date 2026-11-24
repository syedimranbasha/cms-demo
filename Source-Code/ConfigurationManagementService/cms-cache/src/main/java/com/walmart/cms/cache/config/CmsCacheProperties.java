package com.walmart.cms.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cms.cache")
public class CmsCacheProperties {

    /** Default TTL for complete effective-configuration snapshots. */
    private Duration effectiveConfigurationTtl = Duration.ofHours(6);

    /** Embedded Redis settings used by the local demo application. */
    private final EmbeddedRedis embeddedRedis = new EmbeddedRedis();

    public EmbeddedRedis getEmbeddedRedis() {
        return embeddedRedis;
    }

    public static class EmbeddedRedis {
        private boolean enabled = true;
        private String host = "127.0.0.1";
        private int port = 6379;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public Duration getEffectiveConfigurationTtl() {
        return effectiveConfigurationTtl;
    }

    public void setEffectiveConfigurationTtl(Duration effectiveConfigurationTtl) {
        this.effectiveConfigurationTtl = effectiveConfigurationTtl;
    }
}

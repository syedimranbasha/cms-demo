package com.walmart.cms.dal.config;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Starts an in-process PostgreSQL server for the local demo and integration tests.
 *
 * The local/test profiles activate this configuration. Do not activate it in
 * production; production should supply an externally managed PostgreSQL DataSource.
 */
@Configuration
@Profile({"local", "test"})
public class EmbeddedPostgresConfiguration {

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        return EmbeddedPostgres.builder()
                .setPort(0)
                .start();
    }

    @Bean
    @Primary
    public DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
        return embeddedPostgres.getPostgresDatabase();
    }
}

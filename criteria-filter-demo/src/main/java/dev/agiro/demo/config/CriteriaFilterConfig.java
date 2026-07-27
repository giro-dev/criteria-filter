package dev.agiro.demo.config;

import dev.agiro.criteriafilter.repository.jpa.PostgresJsonbOperatorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for criteria-filter library.
 * Registers the PostgreSQL JSONB operator handler.
 */
@Configuration
public class CriteriaFilterConfig {

    @Bean
    public PostgresJsonbOperatorHandler postgresJsonbOperatorHandler() {
        return new PostgresJsonbOperatorHandler();
    }
}

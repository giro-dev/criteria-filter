package dev.agiro.jsonbit;

import dev.agiro.criteriafilter.repository.jpa.PostgresJsonbOperatorHandler;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class JsonbTestApplication {

    @Bean
    PostgresJsonbOperatorHandler postgresJsonbOperatorHandler() {
        return new PostgresJsonbOperatorHandler();
    }
}

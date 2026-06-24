package dev.yashpratap.llmgateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * General-purpose application bean configuration.
 *
 * <p>Provides shared infrastructure beans used across multiple layers of the application,
 * such as the configured Jackson {@link ObjectMapper}.</p>
 */
@Configuration
public class AppConfig {

    /**
     * Configures and exposes the shared Jackson {@link ObjectMapper}.
     *
     * <p>Registers the JSR-310 date/time module and disables timestamp serialisation
     * so that {@code LocalDateTime} values are written as ISO-8601 strings.</p>
     *
     * @return the configured {@link ObjectMapper} instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

package dev.yashpratap.llmgateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yashpratap.llmgateway.tenant.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * Spring Security configuration for the LLM Gateway.
 *
 * <p>The gateway is a fully stateless JSON API: CSRF protection, HTTP Basic, and
 * form login are all disabled. Every protected request must carry a valid
 * {@code Authorization: Bearer <api-key>} header validated by {@link ApiKeyFilter}.</p>
 *
 * <p><strong>Security note:</strong> {@code POST /api/v1/admin/tenants} is intentionally
 * open for development bootstrap. This <em>MUST</em> be secured before production deployment
 * by one of the following approaches:</p>
 * <ol>
 *   <li>Restrict the endpoint with {@code @Profile("dev")} so it is absent in production.</li>
 *   <li>Add an admin secret header check in the controller.</li>
 *   <li>Move tenant provisioning to a separate secured admin tool.</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyAuthenticationProvider apiKeyAuthenticationProvider;
    private final TenantContext tenantContext;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the security configuration with its required dependencies.
     *
     * @param apiKeyAuthenticationProvider the provider that validates hashed API keys
     * @param tenantContext                request-scoped context populated on successful auth
     * @param objectMapper                 Jackson mapper used by the filter for error responses
     */
    public SecurityConfig(ApiKeyAuthenticationProvider apiKeyAuthenticationProvider,
                          TenantContext tenantContext,
                          ObjectMapper objectMapper) {
        this.apiKeyAuthenticationProvider = apiKeyAuthenticationProvider;
        this.tenantContext = tenantContext;
        this.objectMapper = objectMapper;
    }

    /**
     * Exposes the {@link AuthenticationManager} as a bean so it can be injected into
     * {@link ApiKeyFilter} and used in integration tests.
     *
     * <p>Uses {@link ProviderManager} with the single {@link ApiKeyAuthenticationProvider},
     * avoiding any dependency on {@code HttpSecurity} and preventing circular bean creation.</p>
     *
     * @return the configured {@link AuthenticationManager}
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(apiKeyAuthenticationProvider));
    }

    /**
     * Builds the primary security filter chain.
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the security configuration fails to build
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        ApiKeyFilter apiKeyFilter = new ApiKeyFilter(authenticationManager(), tenantContext, objectMapper);

        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/api/v1/health/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/admin/tenants").permitAll()
                .anyRequest().authenticated());

        return http.build();
    }
}

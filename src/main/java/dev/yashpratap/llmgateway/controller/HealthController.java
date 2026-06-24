package dev.yashpratap.llmgateway.controller;

import dev.yashpratap.llmgateway.common.ApiResponse;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.provider.ProviderRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller that exposes provider health status.
 *
 * <p>Used by load balancers, monitoring tools, and the frontend dashboard
 * to display which upstream providers are currently reachable.</p>
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final ProviderRegistry providerRegistry;

    /**
     * Constructs the controller with its provider registry dependency.
     *
     * @param providerRegistry registry that holds all active provider beans
     */
    public HealthController(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    /**
     * Returns the health status of every registered provider.
     *
     * @return {@code 200 OK} with a {@link ProviderHealthResponse} containing
     *         provider names mapped to {@code "UP"} or {@code "DOWN"}
     */
    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<ProviderHealthResponse>> getProviderHealth() {
        Map<String, String> health = providerRegistry.getAllProviders().stream()
                .collect(Collectors.toMap(
                        p -> p.name().name(),
                        p -> p.isHealthy() ? "UP" : "DOWN"));
        ProviderHealthResponse response = new ProviderHealthResponse(health, LocalDateTime.now());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

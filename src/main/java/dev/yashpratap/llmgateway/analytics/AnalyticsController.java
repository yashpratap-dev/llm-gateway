package dev.yashpratap.llmgateway.analytics;

import dev.yashpratap.llmgateway.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing admin-level analytics endpoints.
 *
 * <p>All endpoints under this controller require admin-level authentication,
 * enforced by Spring Security in M2. Aggregation queries are implemented in M8.</p>
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Constructs the controller with its service dependency.
     *
     * @param analyticsService the analytics aggregation service
     */
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Returns an aggregated analytics summary for all tenants.
     *
     * @return a {@code 200 OK} response containing the analytics payload
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Void>> getAnalytics() {
        return ResponseEntity.ok(new ApiResponse<>(true, null, "Analytics endpoint available", null));
    }
}

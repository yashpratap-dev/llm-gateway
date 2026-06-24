package dev.yashpratap.llmgateway.tenant;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Application event published each time an API key successfully authenticates a request.
 *
 * <p>Handled asynchronously by {@link ApiKeyUsedEventListener} to update the
 * {@code last_used_at} column without adding latency to the request path.</p>
 *
 * @param apiKeyId the UUID of the API key that was used
 * @param usedAt   the timestamp of the authentication event
 */
public record ApiKeyUsedEvent(UUID apiKeyId, LocalDateTime usedAt) {
}

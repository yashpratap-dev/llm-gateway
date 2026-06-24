package dev.yashpratap.llmgateway.controller;

import dev.yashpratap.llmgateway.common.ApiResponse;
import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.ChatResponse;
import dev.yashpratap.llmgateway.provider.GatewayMeta;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.routing.RoutingService;
import dev.yashpratap.llmgateway.routing.RoutingStrategy;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes the primary chat completions endpoint.
 *
 * <p>Accepts an OpenAI-compatible {@code POST /api/v1/chat/completions} request,
 * routes it to the appropriate provider via {@link RoutingService}, and returns the
 * completion wrapped in the gateway's standard response envelope.</p>
 *
 * <p>The routing strategy defaults to {@link RoutingStrategy#PRIORITY}.
 * Per-tenant strategy selection is wired in M4 once the routing policy is read
 * from {@link dev.yashpratap.llmgateway.domain.RoutingPolicy}.</p>
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final RoutingService routingService;

    /**
     * Constructs the controller with its routing dependency.
     *
     * @param routingService service responsible for selecting the downstream provider
     */
    public ChatController(RoutingService routingService) {
        this.routingService = routingService;
    }

    /**
     * Handles synchronous chat completion requests.
     *
     * <p>Selects a provider, forwards the request, and augments the response with
     * gateway metadata (provider name and measured latency).</p>
     *
     * @param request the provider-agnostic chat completion request body
     * @return {@code 200 OK} with the completed response wrapped in {@link ApiResponse}
     */
    @PostMapping("/completions")
    public ResponseEntity<ApiResponse<ChatResponse>> complete(
            @Valid @RequestBody ChatRequest request) {

        RoutingStrategy strategy = RoutingStrategy.PRIORITY;
        LLMProvider provider = routingService.route(request, strategy);

        long startMs = System.currentTimeMillis();
        ChatResponse response = provider.generate(request);
        long latencyMs = System.currentTimeMillis() - startMs;

        ChatResponse finalResponse = new ChatResponse(
                response.id(),
                response.model(),
                provider.name().name(),
                response.choices(),
                response.usage(),
                new GatewayMeta(provider.name().name(), false, latencyMs));

        return ResponseEntity.ok(ApiResponse.success(finalResponse));
    }
}

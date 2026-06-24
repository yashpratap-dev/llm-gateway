package dev.yashpratap.llmgateway.streaming;

import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Component that bridges a reactive {@link LLMProvider} stream to a Servlet-based
 * Server-Sent Events connection.
 *
 * <p>Subscribes to the provider's {@code Flux<ChatChunk>} and writes each delta
 * as an SSE event to the client. Handles backpressure and connection cleanup.
 * Full implementation in M4.</p>
 */
@Component
public class StreamingHandler {

    /**
     * Constructs the streaming handler.
     */
    public StreamingHandler() {
    }

    /**
     * Opens an SSE connection and begins streaming completions from the given provider.
     *
     * @param request  the streaming chat request ({@code stream = true})
     * @param provider the provider selected by the routing layer
     * @return a configured {@link SseEmitter} that the controller returns to the client
     */
    public SseEmitter handleStream(ChatRequest request, LLMProvider provider) {
        SseEmitter emitter = new SseEmitter();
        emitter.complete();
        return emitter;
    }
}

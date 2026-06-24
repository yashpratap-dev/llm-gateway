package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.provider.ChatRequest;
import dev.yashpratap.llmgateway.provider.LLMProvider;
import dev.yashpratap.llmgateway.provider.Message;
import dev.yashpratap.llmgateway.provider.ProviderName;
import dev.yashpratap.llmgateway.provider.ProviderRegistry;
import dev.yashpratap.llmgateway.provider.exception.ProviderException;
import dev.yashpratap.llmgateway.routing.CostRouter;
import dev.yashpratap.llmgateway.routing.PriorityRouter;
import dev.yashpratap.llmgateway.routing.RoutingService;
import dev.yashpratap.llmgateway.routing.RoutingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoutingService}.
 */
@ExtendWith(MockitoExtension.class)
class RoutingServiceTest {

    @Mock
    ProviderRegistry providerRegistry;

    @Mock
    LLMProvider groqProvider;

    @Mock
    LLMProvider openaiProvider;

    RoutingService routingService;

    @BeforeEach
    void setUp() {
        PriorityRouter priorityRouter = new PriorityRouter();
        CostRouter costRouter = new CostRouter();
        routingService = new RoutingService(providerRegistry, List.of(priorityRouter, costRouter));
        lenient().when(groqProvider.name()).thenReturn(ProviderName.GROQ);
        lenient().when(openaiProvider.name()).thenReturn(ProviderName.OPENAI);
    }

    @Test
    void testRoute_priorityStrategy_returnsGroqFirst() {
        when(providerRegistry.getHealthyProviders())
                .thenReturn(List.of(openaiProvider, groqProvider));

        LLMProvider result = routingService.route(mockRequest(), RoutingStrategy.PRIORITY);

        assertThat(result.name()).isEqualTo(ProviderName.GROQ);
    }

    @Test
    void testRoute_noHealthyProviders_throwsProviderException() {
        when(providerRegistry.getHealthyProviders()).thenReturn(List.of());

        assertThatThrownBy(() -> routingService.route(mockRequest(), RoutingStrategy.PRIORITY))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("No healthy providers");
    }

    @Test
    void testRoute_costStrategy_returnsCheapestProvider() {
        when(providerRegistry.getHealthyProviders())
                .thenReturn(List.of(openaiProvider, groqProvider));

        LLMProvider result = routingService.route(mockRequest(), RoutingStrategy.COST);

        assertThat(result.name()).isEqualTo(ProviderName.GROQ);
    }

    private ChatRequest mockRequest() {
        return new ChatRequest("auto", List.of(new Message("user", "test")), false, Map.of());
    }
}

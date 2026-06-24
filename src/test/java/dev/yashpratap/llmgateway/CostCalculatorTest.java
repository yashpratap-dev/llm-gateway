package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.billing.CostCalculator;
import dev.yashpratap.llmgateway.billing.ModelPricingRepository;
import dev.yashpratap.llmgateway.domain.ModelPricing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CostCalculator}.
 */
@ExtendWith(MockitoExtension.class)
class CostCalculatorTest {

    @Mock
    private ModelPricingRepository modelPricingRepository;

    private CostCalculator costCalculator;

    @BeforeEach
    void setUp() {
        costCalculator = new CostCalculator(modelPricingRepository);
    }

    @Test
    void testCalculate_knownModel_returnsCorrectCost() {
        ModelPricing pricing = new ModelPricing();
        pricing.setInputCostPer1k(new BigDecimal("0.000059"));
        pricing.setOutputCostPer1k(new BigDecimal("0.000079"));

        when(modelPricingRepository.findByProviderAndModelName("GROQ", "llama-3.3-70b-versatile"))
                .thenReturn(Optional.of(pricing));

        // 1000 prompt tokens × 0.000059 + 500 completion tokens × 0.000079
        // = 0.000059 + 0.0000395 = 0.0000985
        double cost = costCalculator.calculate("GROQ", "llama-3.3-70b-versatile", 1000, 500);

        assertThat(cost).isCloseTo(0.0000985, within(1e-9));
    }

    @Test
    void testCalculate_unknownModel_returnsZero() {
        when(modelPricingRepository.findByProviderAndModelName("UNKNOWN", "no-such-model"))
                .thenReturn(Optional.empty());

        double cost = costCalculator.calculate("UNKNOWN", "no-such-model", 1000, 1000);

        assertThat(cost).isEqualTo(0.0);
    }
}

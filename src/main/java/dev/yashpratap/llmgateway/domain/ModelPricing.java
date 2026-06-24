package dev.yashpratap.llmgateway.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA entity storing per-model pricing used to calculate request costs.
 *
 * <p>Costs are expressed in USD per 1 000 tokens and seeded by the Flyway migration.
 * The combination of {@code provider} and {@code modelName} must be unique.</p>
 */
@Entity
@Table(name = "model_pricing")
@Getter
@Setter
@NoArgsConstructor
public class ModelPricing {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Provider identifier (e.g. GROQ, OPENAI). */
    @Column(nullable = false)
    private String provider;

    /** Model identifier as used by the provider API. */
    @Column(nullable = false)
    private String modelName;

    /** USD cost per 1 000 input (prompt) tokens. */
    @Column(name = "input_cost_per_1k", nullable = false, precision = 10, scale = 6)
    private BigDecimal inputCostPer1k;

    /** USD cost per 1 000 output (completion) tokens. */
    @Column(name = "output_cost_per_1k", nullable = false, precision = 10, scale = 6)
    private BigDecimal outputCostPer1k;
}

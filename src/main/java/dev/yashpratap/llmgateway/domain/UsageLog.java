package dev.yashpratap.llmgateway.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity recording an individual LLM request processed by the gateway.
 *
 * <p>Written asynchronously after each request completes. The tenant and API key
 * are stored as plain UUIDs (no FK) so that log rows are never affected by
 * cascading deletes on the parent tables.</p>
 */
@Entity
@Table(name = "usage_logs")
@Getter
@Setter
@NoArgsConstructor
public class UsageLog {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Tenant that issued the request. */
    @Column(nullable = false)
    private UUID tenantId;

    /** API key used to authenticate the request. */
    @Column(nullable = false)
    private UUID apiKeyId;

    /** Provider that served the request (e.g. GROQ, OPENAI). */
    @Column(nullable = false)
    private String provider;

    /** Model identifier as sent to the downstream provider. */
    @Column(nullable = false)
    private String model;

    /** Number of input tokens billed. */
    @Column(nullable = false)
    private int promptTokens;

    /** Number of output tokens billed. */
    @Column(nullable = false)
    private int completionTokens;

    /** Calculated USD cost for this request. */
    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal costUsd;

    /** End-to-end gateway latency in milliseconds. */
    @Column(nullable = false)
    private long latencyMs;

    /** Whether the response was served from the semantic cache. */
    @Column(nullable = false)
    private boolean cacheHit;

    /** Request outcome: SUCCESS, RATE_LIMITED, BUDGET_EXCEEDED, PROVIDER_ERROR. */
    @Column(nullable = false)
    private String status;

    /** Timestamp when the request was received. */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

package dev.yashpratap.llmgateway.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity tracking the spending budget for a tenant within a billing period.
 *
 * <p>The gateway checks this record before forwarding a request and rejects the
 * request with a {@code 402 Payment Required} when {@code spentUsd >= limitUsd}.</p>
 */
@Entity
@Table(name = "budgets")
@Getter
@Setter
@NoArgsConstructor
public class Budget {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Owning tenant. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Billing period granularity: DAILY or MONTHLY. */
    @Column(nullable = false)
    private String period;

    /** Maximum USD spend allowed within the period. */
    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal limitUsd;

    /** Accumulated USD spend so far in the current period. */
    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal spentUsd;

    /** Timestamp when the counter resets for the next period. */
    @Column(nullable = false)
    private LocalDateTime resetsAt;
}

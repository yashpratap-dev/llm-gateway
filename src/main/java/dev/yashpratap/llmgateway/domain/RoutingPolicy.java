package dev.yashpratap.llmgateway.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity storing a tenant's preferred LLM routing strategy.
 *
 * <p>There is at most one routing policy per tenant (enforced by the UNIQUE constraint
 * on {@code tenant_id}). If no policy exists the gateway falls back to PRIORITY routing.</p>
 */
@Entity
@Table(name = "routing_policies")
@Getter
@Setter
@NoArgsConstructor
public class RoutingPolicy {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Owning tenant. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** Routing algorithm: PRIORITY, COST, or LATENCY. */
    @Column(nullable = false)
    private String strategy;

    /** Timestamp of the last strategy change. */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

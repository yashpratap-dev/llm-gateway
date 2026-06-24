package dev.yashpratap.llmgateway.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing an API key issued to a tenant.
 *
 * <p>The raw key is never stored; only its SHA-256 {@code keyHash} and a short
 * {@code keyPrefix} (used to identify the key family in logs) are persisted.</p>
 */
@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
public class ApiKey {

    /** Surrogate primary key. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Owning tenant. Loaded lazily by default; eagerly loaded via entity graph in {@link dev.yashpratap.llmgateway.tenant.ApiKeyRepository}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    /** SHA-256 hash of the raw API key. Used for constant-time lookup. */
    @Column(nullable = false, unique = true)
    private String keyHash;

    /** Short prefix of the raw key shown in the UI (e.g. {@code lgw_a1b2}). */
    @Column(nullable = false)
    private String keyPrefix;

    /** Descriptive label set by the tenant. */
    @Column(nullable = false)
    private String name;

    /** Lifecycle status stored as its enum name string. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiKeyStatus status = ApiKeyStatus.ACTIVE;

    /** Timestamp when this key was created. */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** Timestamp of the most recent authenticated request using this key. */
    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

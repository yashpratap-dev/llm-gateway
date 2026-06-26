package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.cache.semantic.SemanticCacheEntry;
import dev.yashpratap.llmgateway.cache.semantic.SemanticCacheRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class SemanticCacheRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    private static JdbcTemplate jdbcTemplate;
    private SemanticCacheRepository repository;

    @BeforeAll
    static void setUpSchema() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(postgres.getJdbcUrl());
        ds.setUsername(postgres.getUsername());
        ds.setPassword(postgres.getPassword());

        jdbcTemplate = new JdbcTemplate(ds);

        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"");
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS tenants (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    name VARCHAR(255) NOT NULL UNIQUE,
                    plan VARCHAR(50) NOT NULL DEFAULT 'FREE',
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS semantic_cache_entries (
                    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
                    model VARCHAR(255) NOT NULL,
                    embedding_model VARCHAR(255) NOT NULL,
                    normalized_prompt TEXT NOT NULL,
                    prompt_hash VARCHAR(64) NOT NULL,
                    embedding vector(1536) NOT NULL,
                    response_json TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    expires_at TIMESTAMP NOT NULL,
                    hit_count INT NOT NULL DEFAULT 0,
                    CONSTRAINT uq_semantic_cache_prompt UNIQUE (tenant_id, model, embedding_model, prompt_hash)
                )
                """);
    }

    @BeforeEach
    void setUp() {
        repository = new SemanticCacheRepository(jdbcTemplate);
        jdbcTemplate.execute("DELETE FROM semantic_cache_entries");
        jdbcTemplate.execute("DELETE FROM tenants");
    }

    private UUID createTenant(String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO tenants (id, name) VALUES (?, ?)", id, name);
        return id;
    }

    private float[] uniformEmbedding(float value) {
        float[] embedding = new float[1536];
        Arrays.fill(embedding, value);
        return embedding;
    }

    @Test
    void testStore_insertsRow() {
        UUID tenantId = createTenant("tenant-store");
        Instant expiresAt = Instant.now().plusSeconds(3600);

        repository.store(tenantId, "gpt-4o-mini", "text-embedding-3-small",
                "hello world", "abc123",
                uniformEmbedding(0.1f), "{\"response\":\"ok\"}", expiresAt);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM semantic_cache_entries WHERE tenant_id = ?",
                Integer.class, tenantId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void testFindBestMatch_aboveThreshold_returnsHit() {
        UUID tenantId = createTenant("tenant-hit");
        float[] embedding = uniformEmbedding(0.1f);
        Instant expiresAt = Instant.now().plusSeconds(3600);

        repository.store(tenantId, "gpt-4o-mini", "text-embedding-3-small",
                "hello world", "abc123", embedding, "{\"response\":\"ok\"}", expiresAt);

        Optional<SemanticCacheEntry> result = repository.findBestMatch(
                tenantId, "gpt-4o-mini", "text-embedding-3-small", embedding, 0.92);

        assertThat(result).isPresent();
        assertThat(result.get().responseJson()).isEqualTo("{\"response\":\"ok\"}");
        assertThat(result.get().similarity()).isGreaterThanOrEqualTo(0.99);
    }

    @Test
    void testFindBestMatch_belowThreshold_returnsEmpty() {
        UUID tenantId = createTenant("tenant-miss");
        float[] storedEmbedding = uniformEmbedding(0.1f);
        // Opposite direction → cosine similarity = -1.0, well below 0.92 threshold
        float[] queryEmbedding = uniformEmbedding(-0.1f);
        Instant expiresAt = Instant.now().plusSeconds(3600);

        repository.store(tenantId, "gpt-4o-mini", "text-embedding-3-small",
                "hello world", "abc123", storedEmbedding, "{\"response\":\"ok\"}", expiresAt);

        Optional<SemanticCacheEntry> result = repository.findBestMatch(
                tenantId, "gpt-4o-mini", "text-embedding-3-small", queryEmbedding, 0.92);

        assertThat(result).isEmpty();
    }

    @Test
    void testFindBestMatch_tenantIsolation_tenantBCannotSeetenantAEntry() {
        UUID tenantA = createTenant("tenant-a");
        UUID tenantB = createTenant("tenant-b");
        float[] embedding = uniformEmbedding(0.1f);
        Instant expiresAt = Instant.now().plusSeconds(3600);

        repository.store(tenantA, "gpt-4o-mini", "text-embedding-3-small",
                "hello world", "abc123", embedding, "{\"response\":\"ok\"}", expiresAt);

        Optional<SemanticCacheEntry> result = repository.findBestMatch(
                tenantB, "gpt-4o-mini", "text-embedding-3-small", embedding, 0.92);

        assertThat(result).isEmpty();
    }
}

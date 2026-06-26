package dev.yashpratap.llmgateway.cache.semantic;

import com.pgvector.PGvector;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SemanticCacheRepository {

    private final JdbcTemplate jdbcTemplate;

    public SemanticCacheRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<SemanticCacheEntry> findBestMatch(
            UUID tenantId, String model, String embeddingModel,
            float[] embedding, double threshold) {
        PGvector vector = new PGvector(embedding);
        String sql = """
                SELECT id, response_json,
                       1 - (embedding <=> ?::vector) AS similarity
                FROM semantic_cache_entries
                WHERE tenant_id = ?
                  AND model = ?
                  AND embedding_model = ?
                  AND expires_at > NOW()
                  AND 1 - (embedding <=> ?::vector) >= ?
                ORDER BY embedding <=> ?::vector
                LIMIT 1
                """;

        List<SemanticCacheEntry> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SemanticCacheEntry(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("response_json"),
                        rs.getDouble("similarity")),
                vector, tenantId, model, embeddingModel, vector, threshold, vector);

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void store(
            UUID tenantId, String model, String embeddingModel,
            String normalizedPrompt, String promptHash,
            float[] embedding, String responseJson, Instant expiresAt) {
        PGvector vector = new PGvector(embedding);
        String sql = """
                INSERT INTO semantic_cache_entries
                    (id, tenant_id, model, embedding_model, normalized_prompt,
                     prompt_hash, embedding, response_json, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (tenant_id, model, embedding_model, prompt_hash)
                DO UPDATE SET
                    hit_count  = semantic_cache_entries.hit_count + 1,
                    expires_at = EXCLUDED.expires_at
                """;

        jdbcTemplate.update(sql,
                UUID.randomUUID(),
                tenantId,
                model,
                embeddingModel,
                normalizedPrompt,
                promptHash,
                vector,
                responseJson,
                Timestamp.from(expiresAt));
    }

    public void incrementHitCount(UUID id) {
        jdbcTemplate.update(
                "UPDATE semantic_cache_entries SET hit_count = hit_count + 1 WHERE id = ?",
                id);
    }
}

package dev.yashpratap.llmgateway.cache.semantic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that deletes expired semantic cache entries from PostgreSQL.
 *
 * <p>Runs on a configurable cron schedule (default: top of every hour).
 * Cron format is Spring 6-field format: seconds minutes hours day month weekday.
 * This differs from standard Linux cron (5 fields — no seconds).</p>
 *
 * <p>Only active when cache.semantic.enabled=true.</p>
 *
 * <p>Best-effort: any exception is caught and logged at WARN level.
 * A failed eviction run does not affect request processing — expired entries
 * are filtered by expires_at in query time anyway.</p>
 */
@Component
@ConditionalOnProperty(prefix = "cache.semantic", name = "enabled",
                       havingValue = "true", matchIfMissing = false)
public class SemanticCacheEvictionScheduler {

    private static final Logger log =
        LoggerFactory.getLogger(SemanticCacheEvictionScheduler.class);

    private final JdbcTemplate jdbcTemplate;

    public SemanticCacheEvictionScheduler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Deletes all semantic cache entries whose expires_at is on or before NOW().
     * Uses {@code <=} to handle exact boundary case where expires_at == NOW().
     *
     * Cron expression is configurable via cache.semantic.eviction.cron.
     * Default: "0 0 * * * *" (top of every hour).
     */
    @Scheduled(cron = "${cache.semantic.eviction.cron:0 0 * * * *}")
    public void evictExpiredEntries() {
        try {
            int deleted = jdbcTemplate.update(
                "DELETE FROM semantic_cache_entries WHERE expires_at <= NOW()"
            );
            log.info("[semantic-cache] eviction complete — deleted {} expired entr{}",
                deleted, deleted == 1 ? "y" : "ies");
        } catch (Exception e) {
            log.warn("[semantic-cache] eviction failed (best-effort): {}", e.getMessage());
        }
    }
}

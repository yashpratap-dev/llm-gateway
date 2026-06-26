package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.cache.semantic.SemanticCacheEvictionScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticCacheEvictionSchedulerTest {

    @Mock JdbcTemplate jdbcTemplate;
    SemanticCacheEvictionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SemanticCacheEvictionScheduler(jdbcTemplate);
    }

    @Test
    void evictExpiredEntries_deletesRows_callsCorrectSql() {
        when(jdbcTemplate.update(anyString())).thenReturn(5);

        scheduler.evictExpiredEntries();

        verify(jdbcTemplate).update(
            "DELETE FROM semantic_cache_entries WHERE expires_at <= NOW()"
        );
    }

    @Test
    void evictExpiredEntries_zeroDeleted_completesNormally() {
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        scheduler.evictExpiredEntries();

        verify(jdbcTemplate).update(anyString());
    }

    @Test
    void evictExpiredEntries_jdbcThrows_doesNotPropagate() {
        when(jdbcTemplate.update(anyString()))
            .thenThrow(new RuntimeException("DB connection lost"));

        assertThatCode(() -> scheduler.evictExpiredEntries())
            .doesNotThrowAnyException();
    }
}

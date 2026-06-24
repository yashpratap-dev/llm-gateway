package dev.yashpratap.llmgateway;

import dev.yashpratap.llmgateway.common.KeyHashUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link KeyHashUtil}.
 */
class KeyHashUtilTest {

    @Test
    void testHash_sameInputSameOutput() {
        String key = "lgw_testkey123";
        String first = KeyHashUtil.hash(key);
        String second = KeyHashUtil.hash(key);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void testHash_differentInputDifferentOutput() {
        String hash1 = KeyHashUtil.hash("lgw_keyA");
        String hash2 = KeyHashUtil.hash("lgw_keyB");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void testHash_returnsHexString() {
        String hash = KeyHashUtil.hash("lgw_somekey");
        assertThat(hash).matches("[0-9a-f]{64}");
    }

    @Test
    void testHash_nullInput_throwsException() {
        assertThatThrownBy(() -> KeyHashUtil.hash(null))
                .isInstanceOf(NullPointerException.class);
    }
}

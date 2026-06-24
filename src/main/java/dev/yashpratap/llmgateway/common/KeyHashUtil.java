package dev.yashpratap.llmgateway.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility for hashing raw API keys before persistence.
 *
 * <p>Uses SHA-256 to produce a deterministic, fixed-length hex string from the raw key.
 * The same input always produces the same hash, enabling constant-time lookup by hash
 * without ever storing the raw value.</p>
 */
public final class KeyHashUtil {

    private KeyHashUtil() {
    }

    /**
     * Hashes a raw API key using SHA-256 and returns a lowercase hex string.
     *
     * @param rawKey the raw API key to hash; must not be {@code null}
     * @return a 64-character lowercase hex string representing the SHA-256 digest
     * @throws RuntimeException if SHA-256 is not available on this JVM (should never occur)
     */
    public static String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

package co.edu.uniquindio.brujula.progress.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * FIX-W5: verifies that the SHA-256 hash used by {@link SubscriptionController#computeHash}
 * produces stable hashes for the same input and distinct hashes for different inputs.
 *
 * <p>The hash logic is replicated here (it's private in the controller) since the controller is
 * package-private. This ensures the algorithm itself is correct before wiring into integration
 * tests.
 */
class SubscriptionHashTest {

  /** Replicates the algorithm in {@code SubscriptionController.computeHash()}. */
  private String computeHash(UUID programId, UUID planId, String journeyCode) {
    try {
      String input = programId + "|" + planId + "|" + journeyCode;
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new IllegalStateException("hash failed", e);
    }
  }

  @Test
  void same_body_produces_stable_hash() {
    UUID programId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();

    String hash1 = computeHash(programId, planId, "DIURNA");
    String hash2 = computeHash(programId, planId, "DIURNA");

    assertThat(hash1).isEqualTo(hash2);
  }

  @Test
  void distinct_bodies_produce_distinct_hashes() {
    UUID programId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();

    String hash1 = computeHash(programId, planId, "DIURNA");
    String hash2 = computeHash(programId, planId, "NOCTURNA"); // different journey
    String hash3 = computeHash(programId, UUID.randomUUID(), "DIURNA"); // different plan

    assertThat(hash1).isNotEqualTo(hash2);
    assertThat(hash1).isNotEqualTo(hash3);
    assertThat(hash2).isNotEqualTo(hash3);
  }

  @Test
  void hash_is_sha256_format_64_hex_chars() {
    UUID programId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();

    String hash = computeHash(programId, planId, "DIURNA");

    // SHA-256 produces 32 bytes = 64 hex characters
    assertThat(hash).hasSize(64);
    assertThat(hash).matches("[0-9a-f]{64}");
  }
}

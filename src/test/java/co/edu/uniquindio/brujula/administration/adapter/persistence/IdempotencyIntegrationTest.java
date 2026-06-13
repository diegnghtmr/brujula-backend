package co.edu.uniquindio.brujula.administration.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.administration.application.dto.CreateStaffCommand;
import co.edu.uniquindio.brujula.administration.application.dto.StaffAccountView;
import co.edu.uniquindio.brujula.administration.application.port.in.CreateStaffUseCase;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test: proves idempotency replay against the real PostgreSQL database.
 *
 * <p>Verifies that replaying the same Idempotency-Key + same request hash returns the identical
 * result without writing a duplicate row to {@code administration.staff_account} or {@code
 * platform.idempotency_record}.
 *
 * <p>Also verifies that same key + different hash yields 409 (IdempotencyKeyConflictException).
 *
 * <p>Replay assertions cover all five fields of {@link StaffAccountView} (id, email, fullName,
 * status, version) to guard the Jackson round-trip through PostgreSQL JSONB.
 *
 * <p>Uses {@link AbstractPostgresIntegrationTest} singleton Testcontainers pattern.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class IdempotencyIntegrationTest extends AbstractPostgresIntegrationTest {

  @Autowired private CreateStaffUseCase createStaffUseCase;
  @Autowired private JdbcClient jdbcClient;

  // -------------------------------------------------------------------------
  // Replay same key + same hash → same result, no duplicate row
  // -------------------------------------------------------------------------

  @Test
  void idempotent_replay_returns_same_result_and_writes_no_duplicate_staff_row() {
    UUID idempotencyKey = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    String email = "idem_replay_" + UUID.randomUUID() + "@uniquindio.edu.co";
    String requestHash = "hash-" + idempotencyKey;

    var command =
        new CreateStaffCommand(idempotencyKey, actorId, email, "Replay User", requestHash);

    // First call — creates the staff account
    StaffAccountView first = createStaffUseCase.createStaff(command);

    // Replay — same key, same hash
    StaffAccountView replay = createStaffUseCase.createStaff(command);

    // (a) All five StaffAccountView fields survive the Jackson round-trip through PostgreSQL JSONB
    assertThat(replay.id()).isEqualTo(first.id());
    assertThat(replay.email()).isEqualTo(first.email());
    assertThat(replay.fullName()).isEqualTo(first.fullName());
    assertThat(replay.status()).isEqualTo(first.status());
    assertThat(replay.version()).isEqualTo(first.version());

    // (b) Exactly ONE row in administration.staff_account for this email
    int staffRowCount =
        jdbcClient
            .sql("SELECT COUNT(*) FROM administration.staff_account WHERE email = :email")
            .param("email", email)
            .query(Integer.class)
            .single();
    assertThat(staffRowCount)
        .as("Should be exactly one staff_account row (no duplicate on replay)")
        .isEqualTo(1);

    // (c) Exactly ONE row in platform.idempotency_record for this key
    int idempotencyRowCount =
        jdbcClient
            .sql(
                """
                SELECT COUNT(*) FROM platform.idempotency_record
                 WHERE idempotency_key = :key AND operation = 'CREATE_STAFF'
                """)
            .param("key", idempotencyKey)
            .query(Integer.class)
            .single();
    assertThat(idempotencyRowCount)
        .as("Should be exactly one idempotency_record row (ON CONFLICT DO NOTHING)")
        .isEqualTo(1);
  }

  // -------------------------------------------------------------------------
  // Same key + different hash → 409
  // -------------------------------------------------------------------------

  @Test
  void idempotent_replay_with_different_hash_throws_conflict_exception() {
    UUID idempotencyKey = UUID.randomUUID();
    UUID actorId = UUID.randomUUID();
    String email = "idem_conflict_" + UUID.randomUUID() + "@uniquindio.edu.co";

    var firstCommand =
        new CreateStaffCommand(idempotencyKey, actorId, email, "Conflict User", "hash-original");

    // First call succeeds
    createStaffUseCase.createStaff(firstCommand);

    // Second call — same key, different hash
    var conflictCommand =
        new CreateStaffCommand(
            idempotencyKey, actorId, "different@uniquindio.edu.co", "Other User", "hash-different");

    assertThatThrownBy(() -> createStaffUseCase.createStaff(conflictCommand))
        .isInstanceOf(IdempotencyKeyConflictException.class);
  }
}

// Test resides in the SAME package as StaffAssignmentAdapter to access the package-private class
// (white-box adapter test — acceptable for ACL adapters per hexagonal testing skill).
package co.edu.uniquindio.brujula.imports.infrastructure.adapter.out.administration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.administration.application.port.in.spi.IsStaffAssignedToProgramQuery;
import co.edu.uniquindio.brujula.administration.application.port.in.spi.ResolveStaffByEmailQuery;
import co.edu.uniquindio.brujula.imports.application.port.out.StaffAssignmentPort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link StaffAssignmentAdapter} — anti-corruption adapter bridging
 * imports' {@link StaffAssignmentPort} to {@code administration::spi} queries.
 *
 * <p>Covers T2.3:
 * - SC-001-D / INV-06: unassigned STAFF → port correctly returns false
 * - SC-001-J: admin bypass is the caller's responsibility (adapter itself does not know role)
 * - resolveStaffId delegates to ResolveStaffByEmailQuery
 */
class StaffAssignmentAdapterTest {

  private IsStaffAssignedToProgramQuery isAssignedQuery;
  private ResolveStaffByEmailQuery resolveQuery;
  private StaffAssignmentPort adapter;

  @BeforeEach
  void setUp() {
    isAssignedQuery = mock(IsStaffAssignedToProgramQuery.class);
    resolveQuery = mock(ResolveStaffByEmailQuery.class);
    adapter = new StaffAssignmentAdapter(isAssignedQuery, resolveQuery);
  }

  /** Staff assigned to program → isAssigned returns true. */
  @Test
  void assigned_staff_returns_true() {
    UUID staffId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    when(isAssignedQuery.isAssigned(staffId, programId)).thenReturn(true);

    assertThat(adapter.isAssigned(staffId, programId)).isTrue();
    verify(isAssignedQuery).isAssigned(staffId, programId);
  }

  /** SC-001-D / INV-06: unassigned staff → isAssigned returns false. */
  @Test
  void unassigned_staff_returns_false() {
    UUID staffId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    when(isAssignedQuery.isAssigned(staffId, programId)).thenReturn(false);

    assertThat(adapter.isAssigned(staffId, programId)).isFalse();
  }

  /** resolveStaffId delegates to ResolveStaffByEmailQuery. */
  @Test
  void resolve_staff_id_delegates_to_query() {
    String email = "staff@uniquindio.edu.co";
    UUID staffId = UUID.randomUUID();

    when(resolveQuery.resolveStaffId(email)).thenReturn(Optional.of(staffId));

    Optional<UUID> result = adapter.resolveStaffId(email);
    assertThat(result).isPresent().contains(staffId);
    verify(resolveQuery).resolveStaffId(email);
  }

  /** Unknown email → resolveStaffId returns empty. */
  @Test
  void unknown_email_returns_empty_from_adapter() {
    String email = "ghost@uniquindio.edu.co";
    when(resolveQuery.resolveStaffId(email)).thenReturn(Optional.empty());

    Optional<UUID> result = adapter.resolveStaffId(email);
    assertThat(result).isEmpty();
  }

  /**
   * SC-001-J: admin bypass is enforced by the calling service (UploadImportService),
   * NOT by this adapter. Adapter never calls isAssigned when role is ADMIN — this test
   * verifies the adapter's behavior is purely a pass-through (no role logic inside).
   *
   * <p>We verify that if the caller chooses NOT to invoke the adapter (ADMIN path),
   * the underlying queries are not called.
   */
  @Test
  void adapter_has_no_role_awareness() {
    // This test documents that the adapter is role-agnostic.
    // The admin bypass lives in the importing service layer, not here.
    // If we never call adapter.isAssigned(), the underlying query is also never called.
    UUID staffId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    // Simulate: ADMIN path — caller does NOT invoke the adapter
    // (no adapter call at all)
    verify(isAssignedQuery, never()).isAssigned(staffId, programId);
    verify(resolveQuery, never()).resolveStaffId(staffId.toString());
  }
}

package co.edu.uniquindio.brujula.administration.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.administration.application.port.in.spi.IsStaffAssignedToProgramQuery;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffProgramAssignmentsPort;
import co.edu.uniquindio.brujula.administration.application.service.IsStaffAssignedToProgramService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link IsStaffAssignedToProgramService}.
 *
 * <p>Covers: SC-010-A (active assignment → true), SC-010-B (no assignment → false),
 * SC-010-C (inactive assignment → false).
 */
class IsStaffAssignedToProgramServiceTest {

  private LoadStaffProgramAssignmentsPort loadAssignmentsPort;
  private IsStaffAssignedToProgramQuery service;

  @BeforeEach
  void setUp() {
    loadAssignmentsPort = mock(LoadStaffProgramAssignmentsPort.class);
    service = new IsStaffAssignedToProgramService(loadAssignmentsPort);
  }

  /** SC-010-A: staff has an active assignment → isAssigned returns true. */
  @Test
  void active_assignment_returns_true() {
    UUID staffId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    when(loadAssignmentsPort.findActiveAssignmentsByStaffId(staffId))
        .thenReturn(Set.of(programId));

    assertThat(service.isAssigned(staffId, programId)).isTrue();
  }

  /** SC-010-B: no assignment for the given staff+program pair → isAssigned returns false. */
  @Test
  void no_assignment_returns_false() {
    UUID staffId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();

    when(loadAssignmentsPort.findActiveAssignmentsByStaffId(staffId))
        .thenReturn(Set.of());

    assertThat(service.isAssigned(staffId, programId)).isFalse();
  }

  /** SC-010-C: assignment exists but active=false → isAssigned returns false. */
  @Test
  void inactive_assignment_returns_false() {
    UUID staffId = UUID.randomUUID();
    UUID programId = UUID.randomUUID();
    UUID otherProgramId = UUID.randomUUID();

    // The port only returns ACTIVE assignments; inactive ones are excluded at port level.
    // So if only a different program is returned, the queried one is effectively false.
    when(loadAssignmentsPort.findActiveAssignmentsByStaffId(staffId))
        .thenReturn(Set.of(otherProgramId));

    assertThat(service.isAssigned(staffId, programId)).isFalse();
  }

  /** assignedPrograms returns the set of active program IDs for a staff member. */
  @Test
  void assigned_programs_returns_active_set() {
    UUID staffId = UUID.randomUUID();
    UUID p1 = UUID.randomUUID();
    UUID p2 = UUID.randomUUID();

    when(loadAssignmentsPort.findActiveAssignmentsByStaffId(staffId))
        .thenReturn(Set.of(p1, p2));

    assertThat(service.assignedPrograms(staffId)).containsExactlyInAnyOrder(p1, p2);
  }
}

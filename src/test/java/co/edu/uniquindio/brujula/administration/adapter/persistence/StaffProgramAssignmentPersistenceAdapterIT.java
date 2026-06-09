package co.edu.uniquindio.brujula.administration.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffPort;
import co.edu.uniquindio.brujula.administration.application.port.out.LoadStaffProgramAssignmentsPort;
import co.edu.uniquindio.brujula.administration.application.port.out.SaveStaffPort;
import co.edu.uniquindio.brujula.administration.domain.StaffAccount;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for staff-program assignment persistence.
 *
 * <p>Uses real Testcontainers PostgreSQL over the V003 table {@code
 * administration.staff_program_assignment}.
 *
 * <p>Covers T2.2 tasks: - find_active_assignments_by_staff_id -
 * find_staff_by_email_returns_correct_account (covered by existing StaffPersistenceAdapterTest;
 * reproduced here for completeness against the spi contract)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class StaffProgramAssignmentPersistenceAdapterIT extends AbstractPostgresIntegrationTest {

  @Autowired private LoadStaffProgramAssignmentsPort loadAssignmentsPort;
  @Autowired private SaveStaffPort saveStaffPort;
  @Autowired private LoadStaffPort loadStaffPort;
  @Autowired private JdbcClient jdbcClient;
  @PersistenceContext private EntityManager entityManager;

  /** T2.2: active assignments returned, inactive ones filtered. */
  @Test
  void find_active_assignments_by_staff_id() {
    // Create a staff account first (FK constraint in staff_program_assignment.staff_id)
    StaffAccount staff =
        StaffAccount.create(
            "spi_test_" + UUID.randomUUID() + "@uniquindio.edu.co", "SPI Test Staff");
    StaffAccount savedStaff = saveStaffPort.save(staff);

    UUID programA = UUID.randomUUID();
    UUID programB = UUID.randomUUID();
    UUID programC = UUID.randomUUID();

    // Flush JPA context so the staff_account row is visible to the JDBC insert (FK constraint)
    entityManager.flush();

    // Insert two active and one inactive assignment
    insertAssignment(savedStaff.getId(), programA, true);
    insertAssignment(savedStaff.getId(), programB, true);
    insertAssignment(savedStaff.getId(), programC, false);

    Set<UUID> active = loadAssignmentsPort.findActiveAssignmentsByStaffId(savedStaff.getId());

    assertThat(active).containsExactlyInAnyOrder(programA, programB);
    assertThat(active).doesNotContain(programC);
  }

  /** T2.2: empty result when staff has no assignments. */
  @Test
  void no_assignments_returns_empty_set() {
    StaffAccount staff =
        StaffAccount.create(
            "no_assign_" + UUID.randomUUID() + "@uniquindio.edu.co", "No Assign Staff");
    StaffAccount savedStaff = saveStaffPort.save(staff);
    entityManager.flush();

    Set<UUID> active = loadAssignmentsPort.findActiveAssignmentsByStaffId(savedStaff.getId());

    assertThat(active).isEmpty();
  }

  /** T2.2: find_staff_by_email_returns_correct_account (spi contract). */
  @Test
  void find_staff_by_email_returns_correct_account() {
    String email = "spi_email_" + UUID.randomUUID() + "@uniquindio.edu.co";
    StaffAccount staff = StaffAccount.create(email, "Email Staff");
    saveStaffPort.save(staff);

    var loaded = loadStaffPort.findByEmail(email);
    assertThat(loaded).isPresent();
    assertThat(loaded.get().getEmail()).isEqualTo(email);
  }

  private void insertAssignment(UUID staffId, UUID programId, boolean active) {
    jdbcClient
        .sql(
            """
            INSERT INTO administration.staff_program_assignment
                (id, staff_id, program_id, active, assigned_at, version)
            VALUES (:id, :staffId, :programId, :active, now(), 0)
            """)
        .param("id", UUID.randomUUID())
        .param("staffId", staffId)
        .param("programId", programId)
        .param("active", active)
        .update();
  }
}

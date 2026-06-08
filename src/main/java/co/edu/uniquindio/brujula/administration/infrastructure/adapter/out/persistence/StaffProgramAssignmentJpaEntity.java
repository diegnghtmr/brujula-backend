package co.edu.uniquindio.brujula.administration.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to {@code administration.staff_program_assignment} (Flyway V003).
 *
 * <p>Read-only from application perspective in Fase 6 (write lifecycle deferred to Fase 2).
 * Optimistic locking via {@code @Version} (DB-006).
 *
 * <p>Package-private — no cross-module access (ARQ-006, ARQ-006b).
 */
@Entity
@Table(name = "staff_program_assignment", schema = "administration")
class StaffProgramAssignmentJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "staff_id", nullable = false, updatable = false)
  private UUID staffId;

  @Column(name = "program_id", nullable = false, updatable = false)
  private UUID programId;

  @Column(name = "active", nullable = false)
  private boolean active;

  @Column(name = "assigned_at", nullable = false, updatable = false)
  private OffsetDateTime assignedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected StaffProgramAssignmentJpaEntity() {}

  UUID getStaffId() {
    return staffId;
  }

  UUID getProgramId() {
    return programId;
  }

  boolean isActive() {
    return active;
  }
}

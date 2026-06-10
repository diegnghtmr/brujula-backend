package co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code catalog.academic_program} table (created by Flyway V010).
 *
 * <p>Package-private per ARQ-006. Schema-qualified table annotation follows the Fase-1 identity
 * pattern (verified working with Boot 4.0.3 / Hibernate 6.x).
 *
 * <p>Optimistic locking via {@code @Version} on the {@code version} column (DB-006/DT-014).
 */
@Entity
@Table(name = "academic_program", schema = "catalog")
class AcademicProgramJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "faculty", nullable = false)
  private String faculty;

  @Column(name = "status", nullable = false)
  private String status;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  // JPA requires a no-arg constructor
  protected AcademicProgramJpaEntity() {}

  AcademicProgramJpaEntity(UUID id, String name, String faculty, String status) {
    this.id = id;
    this.name = name;
    this.faculty = faculty;
    this.status = status;
  }

  UUID getId() {
    return id;
  }

  String getName() {
    return name;
  }

  String getFaculty() {
    return faculty;
  }

  String getStatus() {
    return status;
  }

  long getVersion() {
    return version;
  }
}

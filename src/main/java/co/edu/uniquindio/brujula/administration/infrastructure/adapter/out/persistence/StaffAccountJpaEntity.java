package co.edu.uniquindio.brujula.administration.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to {@code administration.staff_account} (Flyway V003).
 *
 * <p>Optimistic locking via {@code @Version}. Package-private — no cross-module access.
 */
@Entity
@Table(name = "staff_account", schema = "administration")
class StaffAccountJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private StaffStatusJpa status;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  protected StaffAccountJpaEntity() {}

  StaffAccountJpaEntity(UUID id, String email, String fullName, StaffStatusJpa status) {
    this.id = id;
    this.email = email;
    this.fullName = fullName;
    this.status = status;
    this.createdAt = OffsetDateTime.now();
    this.updatedAt = OffsetDateTime.now();
  }

  UUID getId() {
    return id;
  }

  String getEmail() {
    return email;
  }

  String getFullName() {
    return fullName;
  }

  StaffStatusJpa getStatus() {
    return status;
  }

  long getVersion() {
    return version;
  }

  void setStatus(StaffStatusJpa status) {
    this.status = status;
    this.updatedAt = OffsetDateTime.now();
  }

  enum StaffStatusJpa {
    ACTIVE,
    INACTIVE
  }
}

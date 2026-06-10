package co.edu.uniquindio.brujula.programcatalog.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code catalog.journey} table (created by Flyway V010).
 *
 * <p>Package-private per ARQ-006. No {@code @Version} on Journey — it is read-only in Fase 2
 * (journeys are seeded data with no optimistic-lock requirement per spec CAT-003).
 */
@Entity
@Table(name = "journey", schema = "catalog")
class JourneyJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "code", nullable = false)
  private String code;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "active", nullable = false)
  private boolean active;

  // JPA requires a no-arg constructor
  protected JourneyJpaEntity() {}

  JourneyJpaEntity(UUID id, String code, String name, boolean active) {
    this.id = id;
    this.code = code;
    this.name = name;
    this.active = active;
  }

  UUID getId() {
    return id;
  }

  String getCode() {
    return code;
  }

  String getName() {
    return name;
  }

  boolean isActive() {
    return active;
  }
}

package co.edu.uniquindio.brujula.administration.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to {@code administration.allowed_email_domain} (Flyway V003). Package-private
 * (ARQ-006).
 */
@Entity
@Table(name = "allowed_email_domain", schema = "administration")
class AllowedEmailDomainJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "domain", nullable = false, unique = true)
  private String domain;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected AllowedEmailDomainJpaEntity() {}

  AllowedEmailDomainJpaEntity(UUID id, String domain) {
    this.id = id;
    this.domain = domain;
    this.createdAt = OffsetDateTime.now();
  }

  UUID getId() {
    return id;
  }

  String getDomain() {
    return domain;
  }
}

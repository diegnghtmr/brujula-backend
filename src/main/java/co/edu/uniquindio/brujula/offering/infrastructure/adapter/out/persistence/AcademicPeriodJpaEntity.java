package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA entity mapping to {@code offering.academic_period}.
 *
 * <p>Package-private. Optimistic locking via {@code @Version}.
 */
@Entity
@Table(name = "academic_period", schema = "offering")
class AcademicPeriodJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(name = "status", nullable = false)
  private String status;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected AcademicPeriodJpaEntity() {}

  UUID getId() {
    return id;
  }

  void setId(UUID id) {
    this.id = id;
  }

  String getName() {
    return name;
  }

  void setName(String name) {
    this.name = name;
  }

  LocalDate getStartDate() {
    return startDate;
  }

  void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  LocalDate getEndDate() {
    return endDate;
  }

  void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  String getStatus() {
    return status;
  }

  void setStatus(String status) {
    this.status = status;
  }

  long getVersion() {
    return version;
  }

  void setVersion(long version) {
    this.version = version;
  }
}

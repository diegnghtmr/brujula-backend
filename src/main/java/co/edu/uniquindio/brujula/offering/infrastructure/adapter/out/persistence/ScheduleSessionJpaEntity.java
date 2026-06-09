package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.UUID;

/**
 * JPA entity mapping to {@code offering.schedule_session} (created by Flyway V016).
 *
 * <p>Package-private per ARQ-006b. No @Version — sessions are cascade-replaced with options.
 */
@Entity
@Table(name = "schedule_session", schema = "offering")
class ScheduleSessionJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "schedule_option_id", nullable = false)
  private ScheduleOptionJpaEntity option;

  @Column(name = "day", nullable = false)
  private String day;

  @Column(name = "starts_at", nullable = false)
  private LocalTime startsAt;

  @Column(name = "ends_at", nullable = false)
  private LocalTime endsAt;

  protected ScheduleSessionJpaEntity() {}

  UUID getId() {
    return id;
  }

  void setId(UUID id) {
    this.id = id;
  }

  ScheduleOptionJpaEntity getOption() {
    return option;
  }

  void setOption(ScheduleOptionJpaEntity option) {
    this.option = option;
  }

  String getDay() {
    return day;
  }

  void setDay(String day) {
    this.day = day;
  }

  LocalTime getStartsAt() {
    return startsAt;
  }

  void setStartsAt(LocalTime startsAt) {
    this.startsAt = startsAt;
  }

  LocalTime getEndsAt() {
    return endsAt;
  }

  void setEndsAt(LocalTime endsAt) {
    this.endsAt = endsAt;
  }
}

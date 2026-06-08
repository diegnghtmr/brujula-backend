package co.edu.uniquindio.brujula.offering.infrastructure.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity mapping to {@code offering.schedule_option} (created by Flyway V016).
 *
 * <p>Package-private per ARQ-006b.
 */
@Entity
@Table(name = "schedule_option", schema = "offering")
class ScheduleOptionJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "offering_id", nullable = false)
  private CourseOfferingJpaEntity offering;

  @Column(name = "label", nullable = false)
  private String label;

  @Column(name = "journey_code", nullable = false)
  private String journeyCode;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @OneToMany(
      mappedBy = "option",
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  @OrderBy("id ASC")
  private Set<ScheduleSessionJpaEntity> sessions = new LinkedHashSet<>();

  protected ScheduleOptionJpaEntity() {}

  UUID getId() { return id; }
  void setId(UUID id) { this.id = id; }

  CourseOfferingJpaEntity getOffering() { return offering; }
  void setOffering(CourseOfferingJpaEntity offering) { this.offering = offering; }

  String getLabel() { return label; }
  void setLabel(String label) { this.label = label; }

  String getJourneyCode() { return journeyCode; }
  void setJourneyCode(String journeyCode) { this.journeyCode = journeyCode; }

  long getVersion() { return version; }
  void setVersion(long version) { this.version = version; }

  Set<ScheduleSessionJpaEntity> getSessions() { return sessions; }
  void setSessions(Set<ScheduleSessionJpaEntity> sessions) { this.sessions = sessions; }
}

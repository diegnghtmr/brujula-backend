package co.edu.uniquindio.brujula.curriculum.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadSequentialTrackPort;
import co.edu.uniquindio.brujula.curriculum.domain.SequentialTrack;
import co.edu.uniquindio.brujula.curriculum.domain.TrackKind;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed adapter implementing {@link LoadSequentialTrackPort}.
 *
 * <p>Package-private. Satisfies: CUR-005.
 */
@Component
class SequentialTrackPersistenceAdapter implements LoadSequentialTrackPort {

  private final SequentialTrackJpaRepository repository;

  SequentialTrackPersistenceAdapter(SequentialTrackJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<SequentialTrack> loadByPlanId(UUID planId, TrackKind kind) {
    List<SequentialTrackJpaEntity> entities;
    if (kind != null) {
      entities = repository.findByPlanIdAndKind(planId, kind.name());
    } else {
      entities = repository.findByPlanId(planId);
    }
    return entities.stream().map(CurriculumPersistenceMapper::toDomain).toList();
  }
}

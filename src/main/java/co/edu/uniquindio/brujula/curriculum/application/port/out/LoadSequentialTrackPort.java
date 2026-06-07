package co.edu.uniquindio.brujula.curriculum.application.port.out;

import co.edu.uniquindio.brujula.curriculum.domain.SequentialTrack;
import co.edu.uniquindio.brujula.curriculum.domain.TrackKind;
import java.util.List;
import java.util.UUID;

/**
 * Output port: load sequential tracks (electiva/profundización) from persistence.
 *
 * <p>Named by need (ISP) per hexagonal architecture conventions. No Spring/framework imports.
 */
public interface LoadSequentialTrackPort {

  /**
   * Load all sequential tracks for a study plan, optionally filtered by kind.
   *
   * @param planId the study plan UUID
   * @param kind optional kind filter; {@code null} means load all kinds
   * @return all matching tracks with their levels and option course IDs
   */
  List<SequentialTrack> loadByPlanId(UUID planId, TrackKind kind);
}

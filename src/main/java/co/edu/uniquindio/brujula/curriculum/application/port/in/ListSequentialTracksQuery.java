package co.edu.uniquindio.brujula.curriculum.application.port.in;

import co.edu.uniquindio.brujula.curriculum.application.dto.SequentialTrackView;
import java.util.List;
import java.util.UUID;

/**
 * Input port: list sequential tracks (electivas / profundizaciones) for a study plan.
 *
 * <p>Backs {@code GET /plans/{planId}/sequential-tracks} (CUR-005). No Spring/framework imports.
 */
public interface ListSequentialTracksQuery {

  /**
   * Return all sequential tracks for the given study plan, optionally filtered by kind.
   *
   * @param planId the plan UUID
   * @param kindFilter optional kind filter string (e.g. "DEEPENING"); {@code null} means all kinds
   * @return the list of {@link SequentialTrackView} DTOs
   */
  List<SequentialTrackView> listSequentialTracks(UUID planId, String kindFilter);
}

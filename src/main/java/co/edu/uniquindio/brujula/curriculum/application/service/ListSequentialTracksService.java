package co.edu.uniquindio.brujula.curriculum.application.service;

import co.edu.uniquindio.brujula.curriculum.application.dto.SequentialTrackView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListSequentialTracksQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadSequentialTrackPort;
import co.edu.uniquindio.brujula.curriculum.domain.SequentialTrack;
import co.edu.uniquindio.brujula.curriculum.domain.SequentialTrackLevel;
import co.edu.uniquindio.brujula.curriculum.domain.TrackKind;
import java.util.List;
import java.util.UUID;

/**
 * Use case implementation: list sequential tracks for a study plan.
 *
 * <p>Delegates to {@link LoadSequentialTrackPort#loadByPlanId} with optional kind filter. No Spring
 * annotations — wired via infrastructure configuration (ARQ-002).
 */
public class ListSequentialTracksService implements ListSequentialTracksQuery {

  private final LoadSequentialTrackPort loadSequentialTrackPort;

  public ListSequentialTracksService(LoadSequentialTrackPort loadSequentialTrackPort) {
    this.loadSequentialTrackPort = loadSequentialTrackPort;
  }

  @Override
  public List<SequentialTrackView> listSequentialTracks(UUID planId, String kindFilter) {
    TrackKind kind = parseKind(kindFilter);
    List<SequentialTrack> tracks = loadSequentialTrackPort.loadByPlanId(planId, kind);
    return tracks.stream().map(this::toView).toList();
  }

  // ---------------------------------------------------------------------------
  // Kind filter parsing
  // ---------------------------------------------------------------------------

  private TrackKind parseKind(String kindFilter) {
    if (kindFilter == null || kindFilter.isBlank()) {
      return null;
    }
    return TrackKind.valueOf(kindFilter.toUpperCase());
  }

  // ---------------------------------------------------------------------------
  // Domain → DTO mapper
  // ---------------------------------------------------------------------------

  private SequentialTrackView toView(SequentialTrack track) {
    List<SequentialTrackView.LevelView> levelViews =
        track.levels().stream().map(this::toLevelView).toList();
    return new SequentialTrackView(
        track.id().value(),
        track.planId(),
        track.kind().name(),
        track.label(),
        track.consecutiveSimultaneityAllowed(),
        track.gatekeeperCourseId(),
        levelViews,
        track.version());
  }

  private SequentialTrackView.LevelView toLevelView(SequentialTrackLevel level) {
    return new SequentialTrackView.LevelView(
        level.id(), level.level(), List.copyOf(level.optionCourseIds()));
  }
}

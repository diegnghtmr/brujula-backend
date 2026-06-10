package co.edu.uniquindio.brujula.curriculum.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Application-layer DTO for a SequentialTrack read view.
 *
 * <p>Returned by query services to REST adapters (ARQ-001 — controllers do not touch domain). No
 * Spring/framework imports.
 *
 * @param id the track UUID
 * @param planId the study plan UUID
 * @param kind the track kind string (PERSONAL_ELECTIVE or DEEPENING)
 * @param label the display label (e.g. "Electiva I", "Profundización")
 * @param consecutiveSimultaneityAllowed whether consecutive levels may be taken simultaneously
 * @param gatekeeperCourseId optional gatekeeper course UUID (nullable; V014 column)
 * @param levels the ordered list of level views
 * @param version optimistic lock version
 */
public record SequentialTrackView(
    UUID id,
    UUID planId,
    String kind,
    String label,
    boolean consecutiveSimultaneityAllowed,
    UUID gatekeeperCourseId,
    List<LevelView> levels,
    long version) {

  /**
   * Nested DTO for one track level.
   *
   * @param id the level row UUID
   * @param level the sequence number (≥ 1)
   * @param optionCourseIds the UUIDs of selectable courses at this level
   */
  public record LevelView(UUID id, int level, List<UUID> optionCourseIds) {}
}

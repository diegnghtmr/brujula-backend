package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing one ordered level within a {@link SequentialTrack}.
 *
 * <p>Corresponds to {@code curriculum.sequential_track_level} rows plus their child option rows in
 * {@code curriculum.sequential_track_level_option}.
 *
 * <p>No Spring/JPA imports.
 *
 * @param id the level row UUID (PK in sequential_track_level)
 * @param level the sequence number (≥ 1)
 * @param optionCourseIds the UUIDs of selectable courses at this level
 */
public record SequentialTrackLevel(UUID id, int level, List<UUID> optionCourseIds) {

  public SequentialTrackLevel {
    Objects.requireNonNull(id, "id must not be null");
    if (level < 1) {
      throw new IllegalArgumentException("level must be >= 1, got: " + level);
    }
    Objects.requireNonNull(optionCourseIds, "optionCourseIds must not be null");
    optionCourseIds = Collections.unmodifiableList(optionCourseIds);
  }
}

package co.edu.uniquindio.brujula.simulation.infrastructure.adapter.out.persistence;

import co.edu.uniquindio.brujula.simulation.domain.SimScheduleSession;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelection;
import co.edu.uniquindio.brujula.simulation.domain.SimulationSelectionId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link SimulationSelection} domain objects and {@link SimulationSelectionJpaEntity}.
 *
 * <p>Sessions are serialized as JSONB: a JSON array of objects with keys {@code day}, {@code
 * startsAt}, {@code endsAt}.
 *
 * <p>Package-private.
 */
@Component
class SimulationSelectionPersistenceMapper {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  SimulationSelection toDomain(SimulationSelectionJpaEntity entity) {
    List<SimScheduleSession> sessions = deserializeSessions(entity.getSessions());
    return SimulationSelection.reconstitute(
        SimulationSelectionId.of(entity.getId()),
        entity.getSimulationId(),
        entity.getOfferingId(),
        entity.getScheduleOptionId(),
        entity.getCourseId(),
        entity.getCourseName(),
        entity.getProgramId(),
        entity.getCredits() != null ? entity.getCredits().doubleValue() : 0.0,
        entity.getJourneyCode(),
        entity.getOfferingStatus(),
        entity.getSelectedOfferingVersion(),
        sessions,
        entity.getStatus(),
        entity.getVersion());
  }

  SimulationSelectionJpaEntity toJpaEntity(SimulationSelection selection) {
    SimulationSelectionJpaEntity entity = new SimulationSelectionJpaEntity();
    entity.setId(selection.getId().value());
    entity.setSimulationId(selection.getSimulationId());
    entity.setOfferingId(selection.getOfferingId());
    entity.setScheduleOptionId(selection.getScheduleOptionId());
    entity.setCourseId(selection.getCourseId());
    entity.setCourseName(selection.getCourseName());
    entity.setProgramId(selection.getProgramId());
    entity.setCredits(BigDecimal.valueOf(selection.getCredits()));
    entity.setJourneyCode(selection.getJourneyCode());
    entity.setOfferingStatus(selection.getOfferingStatus());
    entity.setSelectedOfferingVersion(selection.getSelectedOfferingVersion());
    entity.setSessions(serializeSessions(selection.getSessions()));
    entity.setStatus(selection.getStatus());
    entity.setVersion(selection.getVersion());
    return entity;
  }

  // -------------------------------------------------------------------------
  // JSONB session serialization helpers
  // -------------------------------------------------------------------------

  private String serializeSessions(List<SimScheduleSession> sessions) {
    try {
      List<Map<String, String>> list =
          sessions.stream()
              .map(
                  s ->
                      Map.of(
                          "day", s.day(),
                          "startsAt", s.startsAt().toString(),
                          "endsAt", s.endsAt().toString()))
              .toList();
      return MAPPER.writeValueAsString(list);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialize sessions to JSON", e);
    }
  }

  private List<SimScheduleSession> deserializeSessions(String json) {
    if (json == null || json.isBlank() || json.equals("[]")) {
      return List.of();
    }
    try {
      List<Map<String, String>> list =
          MAPPER.readValue(json, new TypeReference<List<Map<String, String>>>() {});
      return list.stream()
          .map(
              m ->
                  new SimScheduleSession(
                      m.get("day"),
                      LocalTime.parse(m.get("startsAt")),
                      LocalTime.parse(m.get("endsAt"))))
          .toList();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to deserialize sessions from JSON: " + json, e);
    }
  }
}

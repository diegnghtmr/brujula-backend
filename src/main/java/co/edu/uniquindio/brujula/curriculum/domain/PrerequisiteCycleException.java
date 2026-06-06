package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.List;
import java.util.UUID;

/**
 * Domain exception thrown when the prerequisite graph of a study plan contains a cycle.
 *
 * <p>Maps to HTTP 422 (Unprocessable Entity) at the REST adapter layer (CUR-003).
 * No Spring/JPA imports (ARQ-002).
 */
public class PrerequisiteCycleException extends RuntimeException {

  public PrerequisiteCycleException(UUID node) {
    super("Prerequisite cycle detected involving course: " + node);
  }

  public PrerequisiteCycleException(List<UUID> cycleNodes) {
    super("Prerequisite cycle detected in graph involving nodes: " + cycleNodes);
  }

  public PrerequisiteCycleException(String message) {
    super(message);
  }
}

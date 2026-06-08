package co.edu.uniquindio.brujula.offering.domain;

import java.util.UUID;

/**
 * Thrown when a requested {@link CourseOffering} does not exist or is not accessible.
 */
public class OfferingNotFoundException extends RuntimeException {

  public OfferingNotFoundException(CourseOfferingId id) {
    super("Course offering not found: " + id.value());
  }

  public OfferingNotFoundException(UUID id) {
    super("Course offering not found: " + id);
  }

  public OfferingNotFoundException(String message) {
    super(message);
  }
}

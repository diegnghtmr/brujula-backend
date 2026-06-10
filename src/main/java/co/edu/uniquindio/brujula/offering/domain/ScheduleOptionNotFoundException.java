package co.edu.uniquindio.brujula.offering.domain;

/** Thrown when a requested {@link ScheduleOption} does not exist. */
public class ScheduleOptionNotFoundException extends RuntimeException {

  public ScheduleOptionNotFoundException(ScheduleOptionId id) {
    super("Schedule option not found: " + id.value());
  }

  public ScheduleOptionNotFoundException(String message) {
    super(message);
  }
}

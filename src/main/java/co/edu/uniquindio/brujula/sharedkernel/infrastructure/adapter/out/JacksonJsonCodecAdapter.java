package co.edu.uniquindio.brujula.sharedkernel.infrastructure.adapter.out;

import co.edu.uniquindio.brujula.sharedkernel.application.port.out.JsonCodecPort;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

/**
 * Jackson-based implementation of {@link JsonCodecPort}.
 *
 * <p>Infrastructure adapter: Jackson is an I/O concern and belongs here, NOT in the application
 * layer. Resolves WARNING-001 + WARNING-003 from the WU1 verify report: the former hand-rolled
 * parser in {@code CreateStaffService} was misplaced in the application layer and did not escape
 * control characters correctly.
 *
 * <p>Jackson handles all RFC 8259 §7 escaping (backslash, double-quote, control characters
 * U+0000–U+001F) correctly by default.
 *
 * <p>Package-private (ARQ-006b — infrastructure adapter internals are not public). Registered as a
 * Spring {@code @Component} for auto-detection. Design: ADR-F3-001.
 */
@Component
class JacksonJsonCodecAdapter implements JsonCodecPort {

  private final ObjectMapper objectMapper;

  JacksonJsonCodecAdapter() {
    this.objectMapper =
        new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  /** Constructor for injection (used in tests and explicit wiring). */
  JacksonJsonCodecAdapter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException("JSON serialization failed for: " + value.getClass().getName(), e);
    }
  }

  @Override
  public <T> T fromJson(String json, Class<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (Exception e) {
      throw new IllegalStateException(
          "JSON deserialization failed for type: " + type.getName(), e);
    }
  }
}

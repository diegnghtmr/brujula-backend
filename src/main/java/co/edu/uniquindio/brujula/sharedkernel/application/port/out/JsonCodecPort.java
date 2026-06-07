package co.edu.uniquindio.brujula.sharedkernel.application.port.out;

/**
 * Cross-module output port: JSON serialization/deserialization.
 *
 * <p>Provides a reusable, infrastructure-backed codec for converting objects to/from JSON. The
 * interface uses only {@code java.*} types so the application layer remains framework-free. The
 * implementation lives in {@code sharedkernel.infrastructure} and uses Jackson.
 *
 * <p>Intended for use by any module that needs to serialize its view objects for the idempotency
 * store (ADR-F3-001). WU1 (administration), WU2 (subscription), and WU3 (course attempt) all reuse
 * this single codec.
 *
 * <p>Dependency rule: this port imports only {@code java.*} — no Spring, no Jackson, no module
 * domain types (sharedkernel {@code allowedDependencies = {}}).
 */
public interface JsonCodecPort {

  /**
   * Serialize the given object to a JSON string.
   *
   * @param value the object to serialize (must be serializable by the underlying implementation)
   * @return compact JSON representation
   * @throws IllegalStateException if serialization fails
   */
  String toJson(Object value);

  /**
   * Deserialize a JSON string into an instance of the given type.
   *
   * @param <T> target type
   * @param json the JSON string to deserialize
   * @param type the target class
   * @return deserialized object
   * @throws IllegalStateException if deserialization fails
   */
  <T> T fromJson(String json, Class<T> type);
}

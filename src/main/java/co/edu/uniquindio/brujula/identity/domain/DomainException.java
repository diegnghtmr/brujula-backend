package co.edu.uniquindio.brujula.identity.domain;

/**
 * Base class for domain exceptions thrown within the identity module.
 *
 * <p>Framework-free (ARQ-002). Infrastructure adapters map these to appropriate HTTP responses.
 */
public class DomainException extends RuntimeException {

  public DomainException(String message) {
    super(message);
  }

  public DomainException(String message, Throwable cause) {
    super(message, cause);
  }
}

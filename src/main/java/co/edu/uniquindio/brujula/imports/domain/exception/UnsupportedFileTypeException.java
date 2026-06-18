package co.edu.uniquindio.brujula.imports.domain.exception;

/**
 * Thrown when an uploaded file has an unsupported content-type or extension.
 *
 * <p>Maps to HTTP 415 Unsupported Media Type at the controller layer.
 */
public class UnsupportedFileTypeException extends RuntimeException {

  private final String contentType;
  private final String filename;

  public UnsupportedFileTypeException(String contentType, String filename) {
    super(
        "Unsupported file type: contentType='%s', filename='%s'. Only .xlsx and .json are allowed."
            .formatted(contentType, filename));
    this.contentType = contentType;
    this.filename = filename;
  }

  public String getContentType() {
    return contentType;
  }

  public String getFilename() {
    return filename;
  }
}

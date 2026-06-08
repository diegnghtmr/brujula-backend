package co.edu.uniquindio.brujula.imports.domain.exception;

/**
 * Thrown when an uploaded file exceeds the configured maximum size limit.
 *
 * <p>Maps to HTTP 413 Content Too Large at the controller layer (WU4).
 */
public class FileSizeExceededException extends RuntimeException {

  private final long fileSize;
  private final long maxSize;

  public FileSizeExceededException(long fileSize, long maxSize) {
    super(
        "File size %d bytes exceeds the configured maximum of %d bytes."
            .formatted(fileSize, maxSize));
    this.fileSize = fileSize;
    this.maxSize = maxSize;
  }

  public long getFileSize() {
    return fileSize;
  }

  public long getMaxSize() {
    return maxSize;
  }
}

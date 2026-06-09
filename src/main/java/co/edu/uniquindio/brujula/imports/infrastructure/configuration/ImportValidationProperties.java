package co.edu.uniquindio.brujula.imports.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for import file validation limits (NFR-002).
 *
 * <p>Externalized via {@code imports.*} in {@code application.yaml}. Defaults are:
 *
 * <ul>
 *   <li>{@code maxFileSizeBytes = 5_242_880} (5 MB)
 *   <li>{@code maxRows = 5000}
 * </ul>
 */
@ConfigurationProperties(prefix = "imports")
public class ImportValidationProperties {

  /** Maximum allowed file size in bytes (default: 5 MB). */
  private long maxFileSizeBytes = 5_242_880L;

  /** Maximum allowed data rows per import file (default: 5000, NFR-001). */
  private int maxRows = 5000;

  public long getMaxFileSizeBytes() {
    return maxFileSizeBytes;
  }

  public void setMaxFileSizeBytes(long maxFileSizeBytes) {
    this.maxFileSizeBytes = maxFileSizeBytes;
  }

  public int getMaxRows() {
    return maxRows;
  }

  public void setMaxRows(int maxRows) {
    this.maxRows = maxRows;
  }
}

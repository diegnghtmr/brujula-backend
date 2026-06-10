package co.edu.uniquindio.brujula.curriculum.domain;

import java.text.Normalizer;
import java.util.Objects;

/**
 * Domain service that deterministically mints internal official course codes.
 *
 * <p>Format: {@code {PROG}-S{sem}-{SLUG}}
 *
 * <ul>
 *   <li>{@code PROG} — program abbreviation (e.g. ISC, LM); provided by the caller.
 *   <li>{@code sem} — the diurna recommended semester (1-based integer).
 *   <li>{@code SLUG} — uppercase, accent-folded, non-alphanumeric-stripped token derived from the
 *       course name (ADR-F2-005).
 * </ul>
 *
 * <p>Deterministic: same name + program + semester always yields the same code. UUIDs remain the
 * primary key; official codes are for human readability (RN-024/025).
 *
 * <p>No Spring/JPA imports (ARQ-002). Pure Java domain service.
 */
public class OfficialCodeMinter {

  /**
   * Mint an official code for a course.
   *
   * @param programCode the program abbreviation (e.g. "ISC", "LM"); must not be null or blank
   * @param semester the diurna recommended semester (1-based); must be ≥ 1
   * @param courseName the course name to derive the slug from; must not be null or blank
   * @return the minted code, e.g. {@code ISC-S2-PROGRAMACION1}
   */
  public String mint(String programCode, int semester, String courseName) {
    if (programCode == null || programCode.isBlank()) {
      throw new IllegalArgumentException("programCode must not be null or blank");
    }
    if (semester <= 0) {
      throw new IllegalArgumentException("semester must be >= 1, got: " + semester);
    }
    if (courseName == null || courseName.isBlank()) {
      throw new IllegalArgumentException("courseName must not be null or blank");
    }
    String slug = normalizeSlug(courseName);
    return programCode.toUpperCase() + "-S" + semester + "-" + slug;
  }

  // ---------------------------------------------------------------------------
  // Slug normalizer (extracted for reuse and testability)
  // ---------------------------------------------------------------------------

  /**
   * Normalize a course name into an uppercase, accent-folded, non-alnum-stripped slug.
   *
   * <p>Steps:
   *
   * <ol>
   *   <li>Unicode NFD decomposition: "ó" → "o" + combining acute.
   *   <li>Strip combining diacritical marks (category Mn).
   *   <li>Handle ñ → N explicitly (NFD decomposes ñ to n + combining tilde, which gets stripped to
   *       n).
   *   <li>Strip all non-alphanumeric characters (spaces, hyphens, punctuation).
   *   <li>Uppercase the result.
   * </ol>
   */
  String normalizeSlug(String name) {
    Objects.requireNonNull(name, "name must not be null");
    // Pre-process: replace ñ/Ñ before NFD decomposition so they survive the stripping
    String normalized = name.replace('ñ', 'n').replace('Ñ', 'N');
    // NFD decomposition + strip combining marks
    normalized =
        Normalizer.normalize(normalized, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}", "");
    // Strip all non-alphanumeric characters (spaces, hyphens, parentheses, etc.)
    normalized = normalized.replaceAll("[^A-Za-z0-9]", "");
    return normalized.toUpperCase();
  }
}

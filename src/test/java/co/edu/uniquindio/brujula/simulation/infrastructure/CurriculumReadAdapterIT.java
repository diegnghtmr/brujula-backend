package co.edu.uniquindio.brujula.simulation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.simulation.application.port.out.CurriculumReadPort;
import co.edu.uniquindio.brujula.simulation.domain.SequentialPredecessor;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * FIX S1 integration test: proves {@link CurriculumReadAdapterIT} resolves the real V014
 * sequential-track gatekeeper data end-to-end against a live PostgreSQL database.
 *
 * <p>Data provenance:
 *
 * <ul>
 *   <li>V012: seeds ISC DEEPENING tracks (e.g. Línea Ingeniería del Software y HCI, track id =
 *       e0a20000-0000-4000-a000-000000000001) with level-option courses (Profundización I:
 *       c0080000-0000-4000-a000-000000000004, Profundización II:
 *       c0080000-0000-4000-a000-000000000005).
 *   <li>V014: sets {@code gatekeeper_course_id = c0050000-0000-4000-a000-000000000005} (Ingeniería
 *       de Software I) on track e0a20000-0000-4000-a000-000000000001.
 * </ul>
 *
 * <p>Traversal path exercised: {@code sequentialPredecessor(courseId, null)} → {@code
 * getCourse(courseId).type()} = "DEEPENING" → {@code getCourse(courseId).planId()} = ISC plan →
 * {@code listSequentialTracks(planId, null)} → find track containing courseId → return gatekeeper.
 *
 * <p>If this test returns empty (no predecessor), the traversal is broken — that is a REAL bug to
 * surface, not to mask with a softer assertion.
 *
 * <p>Uses Testcontainers PostgreSQL + Flyway V011..V018 (shared via
 * AbstractPostgresIntegrationTest).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CurriculumReadAdapterIT extends AbstractPostgresIntegrationTest {

  @Autowired private CurriculumReadPort curriculumReadPort;

  // V012 UUIDs for the ISC "Línea Ingeniería del Software y HCI" DEEPENING track
  // Profundización I (ISC-S8-PROFUNDIZACIONI) — a level-1 course in the track
  static final UUID PROFUNDIZACION_I_COURSE_ID =
      UUID.fromString("c0080000-0000-4000-a000-000000000004");

  // Expected gatekeeper set by V014: Ingeniería de Software I
  // (ISC-S5-INGENIERIADESOFTWAREI, track e0a20000-0000-4000-a000-000000000001)
  static final UUID EXPECTED_GATEKEEPER_ID =
      UUID.fromString("c0050000-0000-4000-a000-000000000005");

  // V012 planId for ISC Plan 21
  static final UUID ISC_PLAN_ID = UUID.fromString("33333333-0000-4000-a000-000000000001");

  // -------------------------------------------------------------------------
  // S1: sequentialPredecessor resolves real V014 gatekeeper (traversal proof)
  // -------------------------------------------------------------------------

  /**
   * Proves the full getCourse→planId→listSequentialTracks→gatekeeper traversal returns the
   * V014-seeded gatekeeper for a DEEPENING course in the ISC Línea de Software y HCI track.
   *
   * <p>If this assertion fails with {@code predecessorCourseId == null}, the traversal is broken
   * and the bug must be fixed — do NOT relax the assertion.
   */
  @Test
  void sequentialPredecessor_deepeningCourseInSoftwareLine_resolvesV014Gatekeeper() {
    // ACT: resolve predecessor for Profundización I (DEEPENING, level 1 of Línea SW+HCI)
    SequentialPredecessor result =
        curriculumReadPort.sequentialPredecessor(PROFUNDIZACION_I_COURSE_ID, null);

    // ASSERT: course type must be DEEPENING (resolved from getCourse)
    assertThat(result.courseType())
        .as("Profundización I must be classified as DEEPENING")
        .isEqualToIgnoringCase("DEEPENING");

    // ASSERT: isSequentialTrackKind() must return true for DEEPENING
    assertThat(result.isSequentialTrackKind())
        .as("isSequentialTrackKind() must be true for DEEPENING")
        .isTrue();

    // ASSERT: hasPredecessor() must return true — V014 sets a gatekeeper
    assertThat(result.hasPredecessor())
        .as("Track e0a20000 has gatekeeper_course_id set by V014 — hasPredecessor() must be true")
        .isTrue();

    // ASSERT: the gatekeeper must be Ingeniería de Software I (V014 source of truth)
    assertThat(result.predecessorCourseId())
        .as(
            "V014 gatekeeper for Línea SW+HCI track must be c0050000-...-5 (Ingeniería de Software I)")
        .isEqualTo(EXPECTED_GATEKEEPER_ID);

    // ASSERT: predecessor name must be non-null and non-blank
    assertThat(result.predecessorCourseName())
        .as("Gatekeeper course name must be resolved via getCourse")
        .isNotBlank();
  }

  /**
   * Proves that a REQUIRED course (not DEEPENING/PERSONAL_ELECTIVE) returns a predecessor-less
   * result — the SequentialTrackEvaluator must not fire for REQUIRED courses.
   */
  @Test
  void sequentialPredecessor_requiredCourse_returnsNoPredecessor() {
    // V012: Matemáticas Generales — REQUIRED, Semester I
    UUID matematicasGeneralesId = UUID.fromString("c0010000-0000-4000-a000-000000000001");

    SequentialPredecessor result =
        curriculumReadPort.sequentialPredecessor(matematicasGeneralesId, null);

    assertThat(result.isSequentialTrackKind())
        .as("REQUIRED courses are not sequential-track kind")
        .isFalse();
    assertThat(result.hasPredecessor())
        .as("REQUIRED courses have no sequential predecessor")
        .isFalse();
  }

  /**
   * Proves that courseCredits() returns the real V012-seeded credit value (not 0 or a default).
   * This indirectly confirms the GetCourse adapter plumbing is live.
   */
  @Test
  void courseCredits_profundizacionI_returnsRealV012Credits() {
    // V012: Profundización I has 3 credits (ISC-S8-PROFUNDIZACIONI)
    int credits = curriculumReadPort.courseCredits(PROFUNDIZACION_I_COURSE_ID);

    assertThat(credits)
        .as("Profundización I (ISC-S8-PROFUNDIZACIONI) must have real V012 credit value > 0")
        .isGreaterThan(0);
  }
}

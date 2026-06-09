package co.edu.uniquindio.brujula.curriculum.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCoursePort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import co.edu.uniquindio.brujula.curriculum.domain.RecommendedSemester;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * TDD T5.3 — Persistence round-trip integration tests for {@code CoursePersistenceAdapter}.
 *
 * <p>Uses the shared Testcontainers PostgreSQL via {@link AbstractPostgresIntegrationTest}. Tests
 * verify:
 *
 * <ul>
 *   <li>save + load round-trips all fields including recommended-semester children and version
 *   <li>update increments @Version (load-bearing proof of optimistic locking)
 *   <li>archive persists ARCHIVED status
 *   <li>stale-version save throws OptimisticLockingFailureException
 * </ul>
 *
 * Satisfies: CUR-001, CUR-002, SC-009-C, SC-009-D, WU5-T5.3.
 */
@SpringBootTest
@Transactional
class CoursePersistenceAdapterIT extends AbstractPostgresIntegrationTest {

  @Autowired private SaveCoursePort saveCoursePort;

  @Autowired private LoadCoursePort loadCoursePort;

  @Autowired private JdbcTemplate jdbcTemplate;

  @PersistenceContext private EntityManager entityManager;

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private UUID insertStudyPlan(String name, int credits) {
    UUID programId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.academic_program (id, name, faculty, status, version) VALUES (?, ?, 'Facultad', 'ACTIVE', 0)",
        programId,
        "Prog-" + UUID.randomUUID());
    UUID planId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, version) VALUES (?, ?, ?, 'ACTIVE', ?, 0)",
        planId,
        programId,
        name,
        credits);
    return planId;
  }

  private UUID insertProgramForPlan(UUID planId) {
    return jdbcTemplate.queryForObject(
        "SELECT program_id FROM catalog.study_plan WHERE id = ?", UUID.class, planId);
  }

  // -------------------------------------------------------------------------
  // T5.3-1: save + load round-trips all fields (incl. recommended semesters + version 0)
  // -------------------------------------------------------------------------

  @Test
  void save_and_load_round_trips_all_fields_including_recommended_semesters() {
    // ARRANGE
    UUID planId = insertStudyPlan("Plan IT Course", 160);
    UUID programId = insertProgramForPlan(planId);

    CourseId courseId = CourseId.generate();
    List<RecommendedSemester> semesters =
        List.of(new RecommendedSemester("DIURNA", 3), new RecommendedSemester("NOCTURNA", 4));

    Course course =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            "MAT-001",
            "Cálculo Diferencial",
            CourseType.REQUIRED,
            4,
            4,
            semesters,
            CourseStatus.INACTIVE,
            0L);

    // ACT
    Course saved = saveCoursePort.save(course);

    // Flush + clear L1 cache so the load is a real DB read
    entityManager.flush();
    entityManager.clear();

    Optional<Course> loaded = loadCoursePort.loadById(courseId);

    // ASSERT — load-bearing: if any field is not persisted, assertion fails
    assertThat(loaded).isPresent();
    Course c = loaded.get();
    assertThat(c.id()).isEqualTo(courseId);
    assertThat(c.planId()).isEqualTo(planId);
    assertThat(c.programId()).isEqualTo(programId);
    assertThat(c.officialCode()).isEqualTo("MAT-001");
    assertThat(c.name()).isEqualTo("Cálculo Diferencial");
    assertThat(c.type()).isEqualTo(CourseType.REQUIRED);
    assertThat(c.credits()).isEqualTo(4);
    assertThat(c.curricularWeeklyHours()).isEqualTo(4);
    assertThat(c.status()).isEqualTo(CourseStatus.INACTIVE); // inserted with INACTIVE status
    // Load-bearing: @Version must start at 0 after first insert
    assertThat(saved.version()).isEqualTo(0L);

    // Load-bearing: recommended semesters must round-trip including journey codes
    assertThat(c.recommendedSemesters()).hasSize(2);
    assertThat(c.recommendedSemesters())
        .anySatisfy(
            rs -> {
              assertThat(rs.journeyCode()).isEqualTo("DIURNA");
              assertThat(rs.semester()).isEqualTo(3);
            });
    assertThat(c.recommendedSemesters())
        .anySatisfy(
            rs -> {
              assertThat(rs.journeyCode()).isEqualTo("NOCTURNA");
              assertThat(rs.semester()).isEqualTo(4);
            });
  }

  // -------------------------------------------------------------------------
  // T5.3-2: update → @Version increments (load-bearing proof of optimistic lock)
  // -------------------------------------------------------------------------

  @Test
  void update_increments_version() {
    // ARRANGE: save initial version 0
    UUID planId = insertStudyPlan("Plan Version Bump", 160);
    UUID programId = insertProgramForPlan(planId);
    CourseId courseId = CourseId.generate();

    Course initial =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            null,
            "Álgebra Lineal",
            CourseType.REQUIRED,
            3,
            3,
            List.of(),
            CourseStatus.INACTIVE,
            0L);

    Course afterInsert = saveCoursePort.save(initial);
    entityManager.flush();
    entityManager.clear();

    // Load-bearing: version must be 0 after insert
    assertThat(afterInsert.version()).isEqualTo(0L);

    // ACT: update with expectedVersion=0 (matching DB) → version bumps to 1
    Course toUpdate =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            null,
            "Álgebra Lineal (Actualizada)",
            CourseType.REQUIRED,
            3,
            3,
            List.of(),
            CourseStatus.ACTIVE,
            0L);

    Course afterUpdate = saveCoursePort.save(toUpdate);
    entityManager.flush();
    entityManager.clear();

    // Load-bearing: @Version MUST be 1 — if @Version is missing from entity, stays 0
    assertThat(afterUpdate.version()).isEqualTo(1L);

    Optional<Course> loaded = loadCoursePort.loadById(courseId);
    assertThat(loaded).isPresent();
    assertThat(loaded.get().version()).isEqualTo(1L);
    assertThat(loaded.get().name()).isEqualTo("Álgebra Lineal (Actualizada)");
  }

  // -------------------------------------------------------------------------
  // T5.3-3: archive persists ARCHIVED status
  // -------------------------------------------------------------------------

  @Test
  void archive_persists_archived_status() {
    UUID planId = insertStudyPlan("Plan Archive IT", 160);
    UUID programId = insertProgramForPlan(planId);
    CourseId courseId = CourseId.generate();

    Course initial =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            null,
            "Física I",
            CourseType.REQUIRED,
            4,
            4,
            List.of(),
            CourseStatus.ACTIVE,
            0L);

    saveCoursePort.save(initial);
    entityManager.flush();
    entityManager.clear();

    // Archive: status = ARCHIVED, expectedVersion = 0
    Course toArchive =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            null,
            "Física I",
            CourseType.REQUIRED,
            4,
            4,
            List.of(),
            CourseStatus.ARCHIVED,
            0L);

    saveCoursePort.save(toArchive);
    entityManager.flush();
    entityManager.clear();

    // Load-bearing: status MUST be ARCHIVED (not deleted)
    Optional<Course> loaded = loadCoursePort.loadById(courseId);
    assertThat(loaded).isPresent();
    assertThat(loaded.get().status()).isEqualTo(CourseStatus.ARCHIVED);
  }

  // -------------------------------------------------------------------------
  // T5.3-4: stale version → OptimisticLockingFailureException
  // -------------------------------------------------------------------------

  @Test
  void stale_version_throws_optimistic_lock_failure() {
    UUID planId = insertStudyPlan("Plan Stale", 160);
    UUID programId = insertProgramForPlan(planId);
    CourseId courseId = CourseId.generate();

    // Insert at version 0
    Course initial =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            null,
            "Química General",
            CourseType.REQUIRED,
            3,
            3,
            List.of(),
            CourseStatus.INACTIVE,
            0L);
    saveCoursePort.save(initial);
    entityManager.flush();
    entityManager.clear();

    // Update once → DB now at version 1
    Course update1 =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            null,
            "Química General v2",
            CourseType.REQUIRED,
            3,
            3,
            List.of(),
            CourseStatus.ACTIVE,
            0L);
    saveCoursePort.save(update1);
    entityManager.flush();
    entityManager.clear();

    // Load-bearing: trying to save with stale version 0 MUST throw
    Course stale =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            null,
            "Química con versión obsoleta",
            CourseType.REQUIRED,
            3,
            3,
            List.of(),
            CourseStatus.ACTIVE,
            0L);

    assertThatThrownBy(
            () -> {
              saveCoursePort.save(stale);
              entityManager.flush();
            })
        .isInstanceOf(OptimisticLockingFailureException.class);
  }

  // -------------------------------------------------------------------------
  // T5.3-5: course without recommended semesters round-trips correctly
  // -------------------------------------------------------------------------

  @Test
  void course_without_semesters_round_trips() {
    UUID planId = insertStudyPlan("Plan No Semesters", 160);
    UUID programId = insertProgramForPlan(planId);
    CourseId courseId = CourseId.generate();

    Course course =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            null,
            "Electiva Libre",
            CourseType.OPTIONAL,
            2,
            2,
            List.of(),
            CourseStatus.INACTIVE,
            0L);

    saveCoursePort.save(course);
    entityManager.flush();
    entityManager.clear();

    Optional<Course> loaded = loadCoursePort.loadById(courseId);
    assertThat(loaded).isPresent();
    assertThat(loaded.get().recommendedSemesters()).isEmpty();
  }
}

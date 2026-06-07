package co.edu.uniquindio.brujula.curriculum.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadDegreeWorkModalityPort;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadSequentialTrackPort;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.DegreeWorkModality;
import co.edu.uniquindio.brujula.curriculum.domain.SequentialTrack;
import co.edu.uniquindio.brujula.curriculum.domain.TrackKind;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * RED → GREEN persistence adapter tests: curriculum JPA round-trip.
 *
 * <p>Covers: course load-by-planId + load-by-id, course requirement load-by-targetCourseId,
 * sequential track load (with/without kind filter, gatekeeper_course_id), degree work modality load.
 *
 * <p>Uses Testcontainers PostgreSQL + Flyway for a real schema.
 * Satisfies: CUR-001/CUR-003/CUR-005/CUR-006, Design §Phase-2 WU4-2.x.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CurriculumPersistenceAdapterTest extends AbstractPostgresIntegrationTest {

  @Autowired private LoadCoursePort loadCoursePort;
  @Autowired private LoadCourseRequirementPort loadCourseRequirementPort;
  @Autowired private LoadSequentialTrackPort loadSequentialTrackPort;
  @Autowired private LoadDegreeWorkModalityPort loadDegreeWorkModalityPort;
  @Autowired private JdbcTemplate jdbcTemplate;
  @PersistenceContext private EntityManager entityManager;

  // ---------------------------------------------------------------------------
  // LoadCoursePort
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void load_course_by_id_returns_correct_course() {
    UUID planId = insertStudyPlan("Plan Test", "ACTIVE", 160);
    UUID courseId = insertCourse(planId, "Programación I", "REQUIRED", 3, 4, "ACTIVE");

    Optional<Course> result = loadCoursePort.loadById(new CourseId(courseId));

    assertThat(result).isPresent();
    Course course = result.get();
    assertThat(course.id().value()).isEqualTo(courseId);
    assertThat(course.planId()).isEqualTo(planId);
    assertThat(course.name()).isEqualTo("Programación I");
    assertThat(course.status()).isEqualTo(CourseStatus.ACTIVE);
    assertThat(course.credits()).isEqualTo(3);
  }

  @Test
  @Transactional
  void load_course_by_nonexistent_id_returns_empty() {
    Optional<Course> result = loadCoursePort.loadById(new CourseId(UUID.randomUUID()));

    assertThat(result).isEmpty();
  }

  @Test
  @Transactional
  void load_courses_by_plan_id_returns_all_plan_courses() {
    UUID planId = insertStudyPlan("Plan Multi-Cursos", "ACTIVE", 180);
    UUID c1 = insertCourse(planId, "Cálculo I", "REQUIRED", 4, 4, "ACTIVE");
    UUID c2 = insertCourse(planId, "Álgebra Lineal", "REQUIRED", 3, 3, "ACTIVE");

    List<Course> courses = loadCoursePort.loadByPlanId(planId);

    assertThat(courses)
        .extracting(c -> c.id().value())
        .containsExactlyInAnyOrder(c1, c2);
  }

  @Test
  @Transactional
  void load_courses_for_unknown_plan_returns_empty_list() {
    List<Course> courses = loadCoursePort.loadByPlanId(UUID.randomUUID());

    assertThat(courses).isEmpty();
  }

  @Test
  @Transactional
  void course_with_recommended_semester_is_mapped_correctly() {
    UUID planId = insertStudyPlan("Plan Semestres", "ACTIVE", 165);
    UUID courseId = insertCourse(planId, "Fundamentos", "REQUIRED", 2, 2, "ACTIVE");
    insertRecommendedSemester(courseId, "DIURNA", 2);

    Optional<Course> result = loadCoursePort.loadById(new CourseId(courseId));

    assertThat(result).isPresent();
    assertThat(result.get().recommendedSemesters()).hasSize(1);
    assertThat(result.get().recommendedSemesters().get(0).journeyCode()).isEqualTo("DIURNA");
    assertThat(result.get().recommendedSemesters().get(0).semester()).isEqualTo(2);
  }

  @Test
  @Transactional
  void course_version_starts_at_zero() {
    UUID planId = insertStudyPlan("Plan Version", "ACTIVE", 140);
    UUID courseId = insertCourse(planId, "Métodos Numéricos", "REQUIRED", 3, 3, "ACTIVE");

    Optional<Course> result = loadCoursePort.loadById(new CourseId(courseId));

    assertThat(result).isPresent();
    assertThat(result.get().version()).isZero();
  }

  // ---------------------------------------------------------------------------
  // LoadCourseRequirementPort
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void load_requirements_by_target_course_id_returns_correct_requirements() {
    UUID planId = insertStudyPlan("Plan Requisitos", "ACTIVE", 150);
    UUID c1 = insertCourse(planId, "Cálculo II", "REQUIRED", 4, 4, "ACTIVE");
    UUID c2 = insertCourse(planId, "Cálculo I", "REQUIRED", 4, 4, "ACTIVE");
    UUID reqId = insertCourseRequirementCoursePassed(c1, c2);

    List<CourseRequirement> reqs = loadCourseRequirementPort.loadByTargetCourseId(c1);

    assertThat(reqs)
        .extracting(r -> r.id().value())
        .containsExactly(reqId);
    assertThat(reqs.get(0).requiredCourseId()).isEqualTo(c2);
  }

  @Test
  @Transactional
  void load_requirements_for_course_with_no_requirements_returns_empty() {
    UUID planId = insertStudyPlan("Plan Sin Req", "ACTIVE", 120);
    UUID courseId = insertCourse(planId, "Electiva Libre", "PERSONAL_ELECTIVE", 3, 3, "ACTIVE");

    List<CourseRequirement> reqs = loadCourseRequirementPort.loadByTargetCourseId(courseId);

    assertThat(reqs).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // LoadSequentialTrackPort
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void load_sequential_tracks_by_plan_id_returns_all_tracks() {
    UUID planId = insertStudyPlan("Plan Tracks", "ACTIVE", 155);
    UUID t1 = insertSequentialTrack(planId, "PERSONAL_ELECTIVE", "Electiva Humanística", false, null);
    UUID t2 = insertSequentialTrack(planId, "DEEPENING", "Profundización en Desarrollo", false, null);

    List<SequentialTrack> tracks = loadSequentialTrackPort.loadByPlanId(planId, null);

    assertThat(tracks)
        .extracting(t -> t.id().value())
        .containsExactlyInAnyOrder(t1, t2);
  }

  @Test
  @Transactional
  void load_sequential_tracks_filtered_by_kind_returns_only_matching_tracks() {
    UUID planId = insertStudyPlan("Plan Kind Filter", "ACTIVE", 160);
    UUID t1 = insertSequentialTrack(planId, "PERSONAL_ELECTIVE", "Electiva", false, null);
    UUID t2 = insertSequentialTrack(planId, "DEEPENING", "Profundización", false, null);

    List<SequentialTrack> deepening = loadSequentialTrackPort.loadByPlanId(planId, TrackKind.DEEPENING);

    assertThat(deepening)
        .extracting(t -> t.id().value())
        .containsExactly(t2);
    assertThat(deepening)
        .extracting(t -> t.id().value())
        .doesNotContain(t1);
  }

  @Test
  @Transactional
  void sequential_track_with_gatekeeper_course_id_is_mapped_correctly() {
    UUID planId = insertStudyPlan("Plan Gatekeeper", "ACTIVE", 170);
    UUID gatekeeperCourse = insertCourse(planId, "Curso Requisito", "REQUIRED", 3, 3, "ACTIVE");
    UUID trackId = insertSequentialTrack(planId, "DEEPENING", "Profundización Especial", true, gatekeeperCourse);

    List<SequentialTrack> tracks = loadSequentialTrackPort.loadByPlanId(planId, TrackKind.DEEPENING);

    assertThat(tracks).hasSize(1);
    assertThat(tracks.get(0).gatekeeperCourseId()).isEqualTo(gatekeeperCourse);
    assertThat(tracks.get(0).consecutiveSimultaneityAllowed()).isTrue();
  }

  @Test
  @Transactional
  void sequential_track_without_gatekeeper_has_null_gatekeeper() {
    UUID planId = insertStudyPlan("Plan No GK", "ACTIVE", 145);
    insertSequentialTrack(planId, "PERSONAL_ELECTIVE", "Electiva Sin GK", false, null);

    List<SequentialTrack> tracks = loadSequentialTrackPort.loadByPlanId(planId, null);

    assertThat(tracks).hasSize(1);
    assertThat(tracks.get(0).gatekeeperCourseId()).isNull();
  }

  @Test
  @Transactional
  void sequential_track_levels_and_options_are_loaded() {
    UUID planId = insertStudyPlan("Plan Track Levels", "ACTIVE", 165);
    UUID courseA = insertCourse(planId, "Opción A", "PERSONAL_ELECTIVE", 3, 3, "ACTIVE");
    UUID courseB = insertCourse(planId, "Opción B", "PERSONAL_ELECTIVE", 3, 3, "ACTIVE");
    UUID trackId = insertSequentialTrack(planId, "PERSONAL_ELECTIVE", "Electiva con Niveles", false, null);
    UUID levelId = insertSequentialTrackLevel(trackId, 1);
    insertSequentialTrackLevelOption(levelId, courseA);
    insertSequentialTrackLevelOption(levelId, courseB);

    List<SequentialTrack> tracks = loadSequentialTrackPort.loadByPlanId(planId, null);

    assertThat(tracks).hasSize(1);
    assertThat(tracks.get(0).levels()).hasSize(1);
    assertThat(tracks.get(0).levels().get(0).optionCourseIds())
        .containsExactlyInAnyOrder(courseA, courseB);
  }

  // ---------------------------------------------------------------------------
  // LoadDegreeWorkModalityPort
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void load_degree_work_modalities_by_plan_id_returns_all_modalities() {
    UUID planId = insertStudyPlan("Plan TdG", "ACTIVE", 160);
    UUID m1 = insertDegreeWorkModality(planId, "Aplicación", 6, 6, "ACTIVE");
    UUID m2 = insertDegreeWorkModality(planId, "Investigación", 6, 6, "ACTIVE");

    List<DegreeWorkModality> modalities = loadDegreeWorkModalityPort.loadByPlanId(planId);

    assertThat(modalities)
        .extracting(m -> m.id().value())
        .containsExactlyInAnyOrder(m1, m2);
  }

  @Test
  @Transactional
  void degree_work_modality_with_null_credits_loads_correctly() {
    UUID planId = insertStudyPlan("Plan TdG Null", "ACTIVE", 155);
    UUID mId = insertDegreeWorkModalityNullCredits(planId, "Pasantía", "ACTIVE");

    List<DegreeWorkModality> modalities = loadDegreeWorkModalityPort.loadByPlanId(planId);

    DegreeWorkModality modality = modalities.stream()
        .filter(m -> m.id().value().equals(mId))
        .findFirst()
        .orElseThrow();
    assertThat(modality.credits()).isNull();
    assertThat(modality.curricularWeeklyHours()).isNull();
    assertThat(modality.name()).isEqualTo("Pasantía");
  }

  @Test
  @Transactional
  void load_degree_work_modalities_for_unknown_plan_returns_empty() {
    List<DegreeWorkModality> modalities = loadDegreeWorkModalityPort.loadByPlanId(UUID.randomUUID());

    assertThat(modalities).isEmpty();
  }

  // ---------------------------------------------------------------------------
  // SQL insert helpers
  // ---------------------------------------------------------------------------

  private UUID insertStudyPlan(String name, String status, int credits) {
    UUID programId = insertProgram("Prog-" + UUID.randomUUID(), "Facultad Test", "ACTIVE");
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, version) "
            + "VALUES (?, ?, ?, ?, ?, 0)",
        id, programId, name, status, credits);
    entityManager.flush();
    return id;
  }

  private UUID insertProgram(String name, String faculty, String status) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.academic_program (id, name, faculty, status, version) "
            + "VALUES (?, ?, ?, ?, 0)",
        id, name, faculty, status);
    return id;
  }

  private UUID insertCourse(UUID planId, String name, String type, int credits, int hours, String status) {
    UUID programId = jdbcTemplate.queryForObject(
        "SELECT program_id FROM catalog.study_plan WHERE id = ?", UUID.class, planId);
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO curriculum.course (id, plan_id, program_id, name, type, credits, curricular_weekly_hours, status, version) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
        id, planId, programId, name, type, credits, hours, status);
    entityManager.flush();
    return id;
  }

  private void insertRecommendedSemester(UUID courseId, String journeyCode, int semester) {
    jdbcTemplate.update(
        "INSERT INTO curriculum.course_recommended_semester (course_id, journey_code, semester) VALUES (?, ?, ?)",
        courseId, journeyCode, semester);
    entityManager.flush();
  }

  private UUID insertCourseRequirementCoursePassed(UUID targetCourseId, UUID requiredCourseId) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO curriculum.course_requirement "
            + "(id, target_course_id, type, required_course_id, status, version) "
            + "VALUES (?, ?, 'COURSE_PASSED', ?, 'ACTIVE', 0)",
        id, targetCourseId, requiredCourseId);
    entityManager.flush();
    return id;
  }

  private UUID insertSequentialTrack(UUID planId, String kind, String label,
      boolean consecutiveSimultaneityAllowed, UUID gatekeeperCourseId) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO curriculum.sequential_track "
            + "(id, plan_id, kind, label, consecutive_simultaneity_allowed, gatekeeper_course_id, version) "
            + "VALUES (?, ?, ?, ?, ?, ?, 0)",
        id, planId, kind, label, consecutiveSimultaneityAllowed, gatekeeperCourseId);
    entityManager.flush();
    return id;
  }

  private UUID insertSequentialTrackLevel(UUID trackId, int level) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO curriculum.sequential_track_level (id, track_id, level) VALUES (?, ?, ?)",
        id, trackId, level);
    entityManager.flush();
    return id;
  }

  private void insertSequentialTrackLevelOption(UUID levelId, UUID courseId) {
    jdbcTemplate.update(
        "INSERT INTO curriculum.sequential_track_level_option (level_id, course_id) VALUES (?, ?)",
        levelId, courseId);
    entityManager.flush();
  }

  private UUID insertDegreeWorkModality(UUID planId, String name, Integer credits, Integer hours, String status) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO curriculum.degree_work_modality (id, plan_id, name, credits, curricular_weekly_hours, status, version) "
            + "VALUES (?, ?, ?, ?, ?, ?, 0)",
        id, planId, name, credits, hours, status);
    entityManager.flush();
    return id;
  }

  private UUID insertDegreeWorkModalityNullCredits(UUID planId, String name, String status) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO curriculum.degree_work_modality (id, plan_id, name, status, version) "
            + "VALUES (?, ?, ?, ?, 0)",
        id, planId, name, status);
    entityManager.flush();
    return id;
  }
}

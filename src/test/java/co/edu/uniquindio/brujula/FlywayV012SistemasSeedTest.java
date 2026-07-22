package co.edu.uniquindio.brujula;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import co.edu.uniquindio.brujula.curriculum.domain.Confidence;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteAcyclicityService;
import co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteGraph;
import co.edu.uniquindio.brujula.curriculum.domain.RecommendedSemester;
import co.edu.uniquindio.brujula.curriculum.domain.RequirementType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Asserts that after V010 + V011 + V012 Flyway migrations have run on a clean PostgreSQL 18
 * database, the Ingeniería de Sistemas y Computación (ISC) seed data satisfies all consistency
 * constraints:
 *
 * <ul>
 *   <li>Exactly 65 courses seeded for the Sistemas plan (57 original + 8 DEEPENING pool electives
 *       from V028, PROP-001-profundizacion-elective-pool)
 *   <li>study_plan.total_credits = 163
 *   <li>Journeys DIURNA (10 sem) and NOCTURNA (11 sem) exist for Sistemas
 *   <li>Exactly 3 degree_work_modality rows for Sistemas
 *   <li>All 65 courses have a non-null official_code
 *   <li>No cycle in the Sistemas course_requirement graph
 *   <li>No forward-semester dependency for DIURNA journey
 *   <li>No forward-semester dependency for NOCTURNA journey
 * </ul>
 */
@SpringBootTest
class FlywayV012SistemasSeedTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  private static final String PROGRAM_NAME = "Ingeniería de Sistemas y Computación";
  private static final PrerequisiteAcyclicityService acyclicityService =
      new PrerequisiteAcyclicityService();

  // ---------------------------------------------------------------------------
  // Credit total
  // ---------------------------------------------------------------------------

  @Test
  void sistemas_study_plan_has_total_credits_163() {
    Integer credits =
        jdbc.queryForObject(
            """
            SELECT sp.total_credits
            FROM catalog.study_plan sp
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND sp.status = 'ACTIVE'
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(credits)
        .as("Sistemas study plan total_credits must be 163 (Plan 21, Acuerdo 075/2017)")
        .isEqualTo(163);
  }

  // ---------------------------------------------------------------------------
  // Course count
  // ---------------------------------------------------------------------------

  @Test
  void sistemas_plan_has_exactly_65_courses() {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.course c
            JOIN catalog.study_plan sp ON c.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(count)
        .as(
            "Sistemas plan must have exactly 65 courses (57 Plan 21 malla diurna + 8 DEEPENING"
                + " pool electives from V028)")
        .isEqualTo(65);
  }

  // ---------------------------------------------------------------------------
  // Journeys DIURNA + NOCTURNA
  // ---------------------------------------------------------------------------

  @Test
  void sistemas_plan_has_diurna_journey() {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM catalog.program_journey pj
            JOIN catalog.academic_program ap ON pj.program_id = ap.id
            JOIN catalog.journey j ON pj.journey_id = j.id
            WHERE ap.name = ?
              AND j.code = 'DIURNA'
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(count)
        .as("Sistemas must have a DIURNA journey associated via program_journey")
        .isEqualTo(1);
  }

  @Test
  void sistemas_plan_has_nocturna_journey() {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM catalog.program_journey pj
            JOIN catalog.academic_program ap ON pj.program_id = ap.id
            JOIN catalog.journey j ON pj.journey_id = j.id
            WHERE ap.name = ?
              AND j.code = 'NOCTURNA'
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(count)
        .as("Sistemas must have a NOCTURNA journey associated via program_journey")
        .isEqualTo(1);
  }

  @Test
  void sistemas_diurna_has_10_semesters_max() {
    Integer maxSem =
        jdbc.queryForObject(
            """
            SELECT MAX(crs.semester)
            FROM curriculum.course_recommended_semester crs
            JOIN curriculum.course c ON crs.course_id = c.id
            JOIN catalog.study_plan sp ON c.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND crs.journey_code = 'DIURNA'
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(maxSem).as("Sistemas DIURNA journey must have max semester 10").isEqualTo(10);
  }

  @Test
  void sistemas_nocturna_has_11_semesters_max() {
    Integer maxSem =
        jdbc.queryForObject(
            """
            SELECT MAX(crs.semester)
            FROM curriculum.course_recommended_semester crs
            JOIN curriculum.course c ON crs.course_id = c.id
            JOIN catalog.study_plan sp ON c.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND crs.journey_code = 'NOCTURNA'
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(maxSem)
        .as("Sistemas NOCTURNA journey must have max semester 11 (inferred distribution)")
        .isEqualTo(11);
  }

  // ---------------------------------------------------------------------------
  // Degree work modalities
  // ---------------------------------------------------------------------------

  @Test
  void sistemas_plan_has_exactly_3_degree_work_modalities() {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.degree_work_modality dwm
            JOIN catalog.study_plan sp ON dwm.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(count)
        .as(
            "Sistemas must have exactly 3 degree work modalities: aplicación, investigación, pasantía")
        .isEqualTo(3);
  }

  @Test
  void sistemas_degree_work_modalities_have_correct_names() {
    List<String> names =
        jdbc.queryForList(
            """
            SELECT dwm.name
            FROM curriculum.degree_work_modality dwm
            JOIN catalog.study_plan sp ON dwm.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
            ORDER BY dwm.name
            """,
            String.class,
            PROGRAM_NAME);

    assertThat(names)
        .as("Sistemas degree work modalities must be: aplicación, investigación, pasantía")
        .containsExactlyInAnyOrder("aplicación", "investigación", "pasantía");
  }

  // ---------------------------------------------------------------------------
  // All courses have a non-null official_code
  // ---------------------------------------------------------------------------

  @Test
  void sistemas_all_courses_have_non_null_official_code() {
    Integer nullCodeCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.course c
            JOIN catalog.study_plan sp ON c.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND c.official_code IS NULL
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(nullCodeCount)
        .as("All Sistemas courses must have a minted official_code")
        .isEqualTo(0);
  }

  @Test
  void sistemas_official_codes_are_unique_per_program() {
    Integer totalCourses =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.course c
            JOIN catalog.study_plan sp ON c.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
            """,
            Integer.class,
            PROGRAM_NAME);

    Integer distinctCodes =
        jdbc.queryForObject(
            """
            SELECT COUNT(DISTINCT c.official_code)
            FROM curriculum.course c
            JOIN catalog.study_plan sp ON c.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(distinctCodes)
        .as("All 65 Sistemas official_codes must be unique per program (V011 UNIQUE constraint)")
        .isEqualTo(totalCourses);
  }

  // ---------------------------------------------------------------------------
  // No cycle in prerequisite graph (DIURNA and NOCTURNA are same graph)
  // ---------------------------------------------------------------------------

  @Test
  void sistemas_prerequisite_graph_is_acyclic() {
    // Load all courses for the Sistemas plan
    List<CourseRow> courseRows = loadCourseRows(PROGRAM_NAME);
    List<RequirementRow> reqRows = loadRequirementRows(PROGRAM_NAME);

    // Build PrerequisiteGraph from requirement edges
    PrerequisiteGraph graph = new PrerequisiteGraph();
    for (RequirementRow req : reqRows) {
      if (req.requiredCourseId() != null) {
        graph.addEdge(req.requiredCourseId(), req.targetCourseId());
      }
    }
    // Register isolated nodes (courses with no requirements)
    for (CourseRow row : courseRows) {
      // addEdge already registers both ends; for isolated nodes we just need the node in the graph
      // PrerequisiteGraph only exposes nodes via addEdge, so we must check that all courses
      // are either sources or targets in at least one edge, or validate the subset of graph nodes.
      // The acyclicity check only iterates graph.nodes() — isolated nodes need not be in the graph.
    }

    assertThatCode(() -> acyclicityService.validateAcyclicity(graph))
        .as("Sistemas prerequisite graph must be a DAG (no cycles)")
        .doesNotThrowAnyException();
  }

  // ---------------------------------------------------------------------------
  // No forward-semester dependency per journey
  // ---------------------------------------------------------------------------

  @Test
  void sistemas_diurna_has_no_forward_semester_dependency() {
    List<CourseRow> courseRows = loadCourseRows(PROGRAM_NAME);
    List<RequirementRow> reqRows = loadRequirementRows(PROGRAM_NAME);

    List<Course> courses = buildCourseDomain(courseRows, "DIURNA");
    List<CourseRequirement> requirements = buildRequirementDomain(reqRows);
    PrerequisiteGraph graph = buildGraph(reqRows);

    assertThatCode(() -> acyclicityService.validate(graph, requirements, courses))
        .as("Sistemas DIURNA journey must have no forward-semester prerequisite dependencies")
        .doesNotThrowAnyException();
  }

  @Test
  void sistemas_nocturna_has_no_forward_semester_dependency() {
    List<CourseRow> courseRows = loadCourseRows(PROGRAM_NAME);
    List<RequirementRow> reqRows = loadRequirementRows(PROGRAM_NAME);

    List<Course> courses = buildCourseDomain(courseRows, "NOCTURNA");
    List<CourseRequirement> requirements = buildRequirementDomain(reqRows);
    PrerequisiteGraph graph = buildGraph(reqRows);

    assertThatCode(() -> acyclicityService.validate(graph, requirements, courses))
        .as("Sistemas NOCTURNA journey must have no forward-semester prerequisite dependencies")
        .doesNotThrowAnyException();
  }

  // ---------------------------------------------------------------------------
  // All seeded requirements have confidence + inferred populated
  // ---------------------------------------------------------------------------

  @Test
  void sistemas_all_course_passed_requirements_have_confidence_and_inferred_set() {
    Integer nullCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.course_requirement cr
            JOIN curriculum.course c ON cr.target_course_id = c.id
            JOIN catalog.study_plan sp ON c.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND cr.type = 'COURSE_PASSED'
              AND (cr.confidence IS NULL OR cr.inferred IS NULL)
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(nullCount)
        .as("All Sistemas COURSE_PASSED requirements must have confidence and inferred populated")
        .isEqualTo(0);
  }

  // ---------------------------------------------------------------------------
  // Sequential tracks present (PERSONAL_ELECTIVE + DEEPENING)
  // ---------------------------------------------------------------------------

  @Test
  void sistemas_plan_has_personal_elective_sequential_track() {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.sequential_track st
            JOIN catalog.study_plan sp ON st.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND st.kind = 'PERSONAL_ELECTIVE'
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(count)
        .as("Sistemas must have at least one PERSONAL_ELECTIVE sequential track")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void sistemas_plan_has_deepening_sequential_tracks() {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.sequential_track st
            JOIN catalog.study_plan sp ON st.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND st.kind = 'DEEPENING'
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(count)
        .as(
            "Sistemas must have at least 3 DEEPENING sequential tracks (one per profundización line)")
        .isGreaterThanOrEqualTo(3);
  }

  // ---------------------------------------------------------------------------
  // Helper: data loading from DB
  // ---------------------------------------------------------------------------

  private record CourseRow(UUID id, String name, String type, int credits, int hours) {}

  private record CourseRecommendedSemRow(UUID courseId, String journeyCode, int semester) {}

  private record RequirementRow(
      UUID id,
      UUID targetCourseId,
      String type,
      UUID requiredCourseId,
      String confidence,
      Boolean inferred) {}

  private List<CourseRow> loadCourseRows(String programName) {
    return jdbc.query(
        """
        SELECT c.id, c.name, c.type, c.credits, c.curricular_weekly_hours
        FROM curriculum.course c
        JOIN catalog.study_plan sp ON c.plan_id = sp.id
        JOIN catalog.academic_program ap ON sp.program_id = ap.id
        WHERE ap.name = ?
        """,
        (rs, n) ->
            new CourseRow(
                UUID.fromString(rs.getString("id")),
                rs.getString("name"),
                rs.getString("type"),
                rs.getInt("credits"),
                rs.getInt("curricular_weekly_hours")),
        programName);
  }

  private List<CourseRecommendedSemRow> loadRecommendedSemRows(
      String programName, String journeyCode) {
    return jdbc.query(
        """
        SELECT crs.course_id, crs.journey_code, crs.semester
        FROM curriculum.course_recommended_semester crs
        JOIN curriculum.course c ON crs.course_id = c.id
        JOIN catalog.study_plan sp ON c.plan_id = sp.id
        JOIN catalog.academic_program ap ON sp.program_id = ap.id
        WHERE ap.name = ?
          AND crs.journey_code = ?
        """,
        (rs, n) ->
            new CourseRecommendedSemRow(
                UUID.fromString(rs.getString("course_id")),
                rs.getString("journey_code"),
                rs.getInt("semester")),
        programName,
        journeyCode);
  }

  private List<RequirementRow> loadRequirementRows(String programName) {
    return jdbc.query(
        """
        SELECT cr.id, cr.target_course_id, cr.type, cr.required_course_id,
               cr.confidence, cr.inferred
        FROM curriculum.course_requirement cr
        JOIN curriculum.course c ON cr.target_course_id = c.id
        JOIN catalog.study_plan sp ON c.plan_id = sp.id
        JOIN catalog.academic_program ap ON sp.program_id = ap.id
        WHERE ap.name = ?
          AND cr.type IN ('COURSE_PASSED', 'SEQUENCE')
        """,
        (rs, n) -> {
          String reqId = rs.getString("required_course_id");
          String conf = rs.getString("confidence");
          boolean inferred = rs.getBoolean("inferred");
          boolean inferredWasNull = rs.wasNull();
          return new RequirementRow(
              UUID.fromString(rs.getString("id")),
              UUID.fromString(rs.getString("target_course_id")),
              rs.getString("type"),
              reqId != null ? UUID.fromString(reqId) : null,
              conf,
              inferredWasNull ? null : inferred);
        },
        programName);
  }

  private List<Course> buildCourseDomain(List<CourseRow> rows, String journeyCode) {
    // Load semester data for the given journey
    Map<UUID, Integer> semesterByCoureId =
        loadRecommendedSemRows(PROGRAM_NAME, journeyCode).stream()
            .collect(
                Collectors.toMap(
                    CourseRecommendedSemRow::courseId, CourseRecommendedSemRow::semester));

    UUID dummyPlanId = UUID.randomUUID();
    UUID dummyProgramId = UUID.randomUUID();

    List<Course> courses = new ArrayList<>();
    for (CourseRow row : rows) {
      List<RecommendedSemester> semesters = new ArrayList<>();
      if (semesterByCoureId.containsKey(row.id())) {
        semesters.add(new RecommendedSemester(journeyCode, semesterByCoureId.get(row.id())));
      }
      courses.add(
          Course.reconstitute(
              new CourseId(row.id()),
              dummyPlanId,
              dummyProgramId,
              null,
              row.name(),
              CourseType.valueOf(row.type()),
              row.credits(),
              row.hours(),
              semesters,
              CourseStatus.ACTIVE,
              0L));
    }
    return courses;
  }

  private List<CourseRequirement> buildRequirementDomain(List<RequirementRow> rows) {
    List<CourseRequirement> reqs = new ArrayList<>();
    for (RequirementRow row : rows) {
      Confidence conf = row.confidence() != null ? Confidence.valueOf(row.confidence()) : null;
      reqs.add(
          CourseRequirement.reconstitute(
              new CourseRequirementId(row.id()),
              row.targetCourseId(),
              RequirementType.valueOf(row.type()),
              row.requiredCourseId(),
              null,
              null,
              conf,
              row.inferred(),
              CourseRequirementStatus.ACTIVE,
              0L));
    }
    return reqs;
  }

  private PrerequisiteGraph buildGraph(List<RequirementRow> rows) {
    PrerequisiteGraph graph = new PrerequisiteGraph();
    for (RequirementRow row : rows) {
      if (row.requiredCourseId() != null) {
        graph.addEdge(row.requiredCourseId(), row.targetCourseId());
      }
    }
    return graph;
  }
}

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
 * Asserts that after V010 + V011 + V012 + V013 Flyway migrations have run on a clean PostgreSQL 18
 * database, the Licenciatura en Lenguas Modernas (LM) seed data satisfies all consistency
 * constraints:
 *
 * <ul>
 *   <li>Exactly 55 courses seeded for the Lenguas plan
 *   <li>study_plan.total_credits = 168
 *   <li>Journeys DIURNA (10 sem) and NOCTURNA (12 sem) exist for Lenguas
 *   <li>All 55 courses have a non-null official_code
 *   <li>All requirement edges have confidence + inferred set; all inferred=true (Lenguas has no
 *       official prereq matrix — all edges are inferred)
 *   <li>No cycle in the Lenguas course_requirement graph
 *   <li>No forward-semester dependency for DIURNA journey
 *   <li>No forward-semester dependency for NOCTURNA journey
 * </ul>
 */
@SpringBootTest
class FlywayV013LenguasSeedTest extends AbstractPostgresIntegrationTest {

  @Autowired private JdbcTemplate jdbc;

  private static final String PROGRAM_NAME = "Licenciatura en Lenguas Modernas";
  private static final PrerequisiteAcyclicityService acyclicityService =
      new PrerequisiteAcyclicityService();

  // ---------------------------------------------------------------------------
  // Credit total
  // ---------------------------------------------------------------------------

  @Test
  void lenguas_study_plan_has_total_credits_168() {
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
        .as("Lenguas study plan total_credits must be 168 (Acuerdo 042/2016)")
        .isEqualTo(168);
  }

  // ---------------------------------------------------------------------------
  // Course count
  // ---------------------------------------------------------------------------

  @Test
  void lenguas_plan_has_exactly_55_courses() {
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
        .as("Lenguas plan must have exactly 55 courses (Acuerdo 042 malla)")
        .isEqualTo(55);
  }

  // ---------------------------------------------------------------------------
  // Journeys DIURNA + NOCTURNA
  // ---------------------------------------------------------------------------

  @Test
  void lenguas_plan_has_diurna_journey() {
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
        .as("Lenguas must have a DIURNA journey associated via program_journey")
        .isEqualTo(1);
  }

  @Test
  void lenguas_plan_has_nocturna_journey() {
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
        .as("Lenguas must have a NOCTURNA journey (approved inferred 12-sem, #1610)")
        .isEqualTo(1);
  }

  @Test
  void lenguas_diurna_has_10_semesters_max() {
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

    assertThat(maxSem).as("Lenguas DIURNA journey must have max semester 10").isEqualTo(10);
  }

  @Test
  void lenguas_nocturna_has_12_semesters_max() {
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
        .as("Lenguas NOCTURNA journey must have max semester 12 (inferred, approved #1610)")
        .isEqualTo(12);
  }

  // ---------------------------------------------------------------------------
  // All courses have a non-null official_code
  // ---------------------------------------------------------------------------

  @Test
  void lenguas_all_courses_have_non_null_official_code() {
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
        .as("All Lenguas courses must have a minted official_code")
        .isEqualTo(0);
  }

  @Test
  void lenguas_official_codes_are_unique_per_program() {
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
        .as("All 55 Lenguas official_codes must be unique per program (V011 UNIQUE constraint)")
        .isEqualTo(totalCourses);
  }

  // ---------------------------------------------------------------------------
  // All COURSE_PASSED requirements have confidence + inferred populated
  //   AND inferred=true for ALL Lenguas edges (no official prereq matrix)
  // ---------------------------------------------------------------------------

  @Test
  void lenguas_all_course_passed_requirements_have_confidence_and_inferred_set() {
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
        .as("All Lenguas COURSE_PASSED requirements must have confidence and inferred populated")
        .isEqualTo(0);
  }

  @Test
  void lenguas_all_requirements_are_inferred_true() {
    Integer notInferredCount =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM curriculum.course_requirement cr
            JOIN curriculum.course c ON cr.target_course_id = c.id
            JOIN catalog.study_plan sp ON c.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND cr.type = 'COURSE_PASSED'
              AND (cr.inferred IS NULL OR cr.inferred = false)
            """,
            Integer.class,
            PROGRAM_NAME);

    assertThat(notInferredCount)
        .as("All Lenguas requirement edges must have inferred=true (no official prereq matrix)")
        .isEqualTo(0);
  }

  // ---------------------------------------------------------------------------
  // No cycle in prerequisite graph
  // ---------------------------------------------------------------------------

  @Test
  void lenguas_prerequisite_graph_is_acyclic() {
    List<RequirementRow> reqRows = loadRequirementRows(PROGRAM_NAME);

    PrerequisiteGraph graph = new PrerequisiteGraph();
    for (RequirementRow req : reqRows) {
      if (req.requiredCourseId() != null) {
        graph.addEdge(req.requiredCourseId(), req.targetCourseId());
      }
    }

    assertThatCode(() -> acyclicityService.validateAcyclicity(graph))
        .as("Lenguas prerequisite graph must be a DAG (no cycles)")
        .doesNotThrowAnyException();
  }

  // ---------------------------------------------------------------------------
  // No forward-semester dependency per journey
  // ---------------------------------------------------------------------------

  @Test
  void lenguas_diurna_has_no_forward_semester_dependency() {
    List<CourseRow> courseRows = loadCourseRows(PROGRAM_NAME);
    List<RequirementRow> reqRows = loadRequirementRows(PROGRAM_NAME);

    List<Course> courses = buildCourseDomain(courseRows, "DIURNA");
    List<CourseRequirement> requirements = buildRequirementDomain(reqRows);
    PrerequisiteGraph graph = buildGraph(reqRows);

    assertThatCode(() -> acyclicityService.validate(graph, requirements, courses))
        .as("Lenguas DIURNA journey must have no forward-semester prerequisite dependencies")
        .doesNotThrowAnyException();
  }

  @Test
  void lenguas_nocturna_has_no_forward_semester_dependency() {
    List<CourseRow> courseRows = loadCourseRows(PROGRAM_NAME);
    List<RequirementRow> reqRows = loadRequirementRows(PROGRAM_NAME);

    List<Course> courses = buildCourseDomain(courseRows, "NOCTURNA");
    List<CourseRequirement> requirements = buildRequirementDomain(reqRows);
    PrerequisiteGraph graph = buildGraph(reqRows);

    assertThatCode(() -> acyclicityService.validate(graph, requirements, courses))
        .as("Lenguas NOCTURNA journey must have no forward-semester prerequisite dependencies")
        .doesNotThrowAnyException();
  }

  // ---------------------------------------------------------------------------
  // Sequential tracks present (DEEPENING + PERSONAL_ELECTIVE)
  // ---------------------------------------------------------------------------

  @Test
  void lenguas_plan_has_deepening_sequential_track_electivas_profesionales() {
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
            "Lenguas must have at least 1 DEEPENING sequential track (Electivas Profesionales I→II→III)")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void lenguas_plan_has_personal_elective_sequential_track() {
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
        .as(
            "Lenguas must have at least 1 PERSONAL_ELECTIVE sequential track (Actividades Formación Personal)")
        .isGreaterThanOrEqualTo(1);
  }

  @Test
  void lenguas_electivas_profesionales_track_has_3_levels() {
    Integer count =
        jdbc.queryForObject(
            """
            SELECT COUNT(stl.id)
            FROM curriculum.sequential_track_level stl
            JOIN curriculum.sequential_track st ON stl.track_id = st.id
            JOIN catalog.study_plan sp ON st.plan_id = sp.id
            JOIN catalog.academic_program ap ON sp.program_id = ap.id
            WHERE ap.name = ?
              AND st.kind = 'DEEPENING'
              AND st.label LIKE '%Electivas Profesionales%'
            """,
            Integer.class, PROGRAM_NAME);

    assertThat(count)
        .as("Lenguas Electivas Profesionales DEEPENING track must have 3 levels (I→II→III)")
        .isEqualTo(3);
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
    Map<UUID, Integer> semesterByCourseId =
        loadRecommendedSemRows(PROGRAM_NAME, journeyCode).stream()
            .collect(
                Collectors.toMap(
                    CourseRecommendedSemRow::courseId, CourseRecommendedSemRow::semester));

    UUID dummyPlanId = UUID.randomUUID();
    UUID dummyProgramId = UUID.randomUUID();

    List<Course> courses = new ArrayList<>();
    for (CourseRow row : rows) {
      List<RecommendedSemester> semesters = new ArrayList<>();
      if (semesterByCourseId.containsKey(row.id())) {
        semesters.add(new RecommendedSemester(journeyCode, semesterByCourseId.get(row.id())));
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

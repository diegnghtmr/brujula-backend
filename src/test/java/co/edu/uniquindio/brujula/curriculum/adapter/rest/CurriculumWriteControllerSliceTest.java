package co.edu.uniquindio.brujula.curriculum.adapter.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ArchiveCourseUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.in.AssemblePlanMallaQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.CreateCourseRequirementUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.in.CreateCourseUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.in.GetCourseQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListCourseRequirementsQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListDegreeWorkModalitiesQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListPlanCoursesQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListSequentialTracksQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.in.UpdateCourseRequirementUseCase;
import co.edu.uniquindio.brujula.curriculum.application.port.in.UpdateCourseUseCase;
import co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementNotFoundException;
import co.edu.uniquindio.brujula.curriculum.domain.PrerequisiteCycleException;
import co.edu.uniquindio.brujula.identity.application.dto.UserProfile;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.programcatalog.application.dto.StudyPlanView;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetStudyPlanQuery;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * WU5 controller slice tests for the 5 curriculum write endpoints.
 *
 * <p>Tests:
 * <ul>
 *   <li>STUDENT → 403 on all write endpoints (role guard)
 *   <li>createCourse → 201 + Location header; idempotency conflict → 409 + $.code=IDEMPOTENCY_KEY_CONFLICT
 *   <li>updateCourse → 200; stale version → 409 + $.code=OPTIMISTIC_LOCK_CONFLICT
 *   <li>archiveCourse → 200; not found → 404
 *   <li>createCourseRequirement → 201; cycle → 422 + $.code=PREREQUISITE_CYCLE
 *   <li>updateCourseRequirement → 200; requirement not found → 404; stale version → 409
 * </ul>
 *
 * <p>DEBT-F6-001 load-bearing: the $.code assertions (OPTIMISTIC_LOCK_CONFLICT, IDEMPOTENCY_KEY_CONFLICT,
 * PREREQUISITE_CYCLE) prove the scoped {@code CurriculumExceptionHandler} wins over any global
 * advice for these exceptions when thrown by CurriculumController. If any other module's handler
 * intercepts first and does NOT set $.code, these assertions fail.
 *
 * Satisfies: WU5-T5.4, CUR-001/CUR-002/CUR-003, SC-009-B/SC-009-C, IMP-009.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class CurriculumWriteControllerSliceTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    // All write use cases and read queries mocked — pure slice/security + exception-mapping test
    @MockitoBean private CreateCourseUseCase createCourseUseCase;
    @MockitoBean private UpdateCourseUseCase updateCourseUseCase;
    @MockitoBean private ArchiveCourseUseCase archiveCourseUseCase;
    @MockitoBean private CreateCourseRequirementUseCase createCourseRequirementUseCase;
    @MockitoBean private UpdateCourseRequirementUseCase updateCourseRequirementUseCase;
    @MockitoBean private GetCurrentUserQuery getCurrentUserQuery;
    @MockitoBean private GetCourseQuery getCourseQuery;
    @MockitoBean private ListPlanCoursesQuery listPlanCoursesQuery;
    @MockitoBean private ListCourseRequirementsQuery listCourseRequirementsQuery;
    @MockitoBean private ListSequentialTracksQuery listSequentialTracksQuery;
    @MockitoBean private ListDegreeWorkModalitiesQuery listDegreeWorkModalitiesQuery;
    @MockitoBean private AssemblePlanMallaQuery assemblePlanMallaQuery;
    @MockitoBean private GetStudyPlanQuery getStudyPlanQuery;

    private MockMvc mockMvc;

    private static final UUID PLAN_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID REQUIREMENT_ID = UUID.randomUUID();
    private static final UUID REQUIRED_COURSE_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final String XSRF_TOKEN = "test-xsrf-token";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    // =========================================================================
    // createCourse — POST /staff/plans/{planId}/courses
    // =========================================================================

    @Test
    void student_gets_403_on_create_course() throws Exception {
        mockMvc.perform(post("/staff/plans/{planId}/courses", PLAN_ID)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createCourseBody())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void staff_creates_course_returns_201_with_location() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        CourseView view = aCourseView(COURSE_ID, PLAN_ID, PROGRAM_ID, "Cálculo I", 0L);

        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(getStudyPlanQuery.getStudyPlan(any(), any()))
            .thenReturn(aStudyPlanView(PLAN_ID, PROGRAM_ID));
        when(createCourseUseCase.createCourse(any())).thenReturn(view);

        mockMvc.perform(post("/staff/plans/{planId}/courses", PLAN_ID)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createCourseBody())
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.name").value("Cálculo I"))
            .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    /**
     * DEBT-F6-001 load-bearing: $.code=IDEMPOTENCY_KEY_CONFLICT proves CurriculumExceptionHandler
     * wins for IdempotencyKeyConflictException thrown from CurriculumController's createCourse.
     */
    @Test
    void idempotency_conflict_returns_409_with_code_on_create_course() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();

        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(getStudyPlanQuery.getStudyPlan(any(), any()))
            .thenReturn(aStudyPlanView(PLAN_ID, PROGRAM_ID));
        when(createCourseUseCase.createCourse(any()))
            .thenThrow(new IdempotencyKeyConflictException(idempotencyKey, "CREATE_COURSE"));

        mockMvc.perform(post("/staff/plans/{planId}/courses", PLAN_ID)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createCourseBody())
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isConflict())
            // DEBT-F6-001: CurriculumExceptionHandler MUST set $.code; if a global handler wins
            // without setProperty("code"), this assertion fails — the test IS the proof
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
    }

    @Test
    void admin_creates_course_returns_201() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        CourseView view = aCourseView(COURSE_ID, PLAN_ID, PROGRAM_ID, "Cálculo I", 0L);

        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(anAdminUserProfile());
        when(getStudyPlanQuery.getStudyPlan(any(), any()))
            .thenReturn(aStudyPlanView(PLAN_ID, PROGRAM_ID));
        when(createCourseUseCase.createCourse(any())).thenReturn(view);

        mockMvc.perform(post("/staff/plans/{planId}/courses", PLAN_ID)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createCourseBody())
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isCreated());
    }

    // =========================================================================
    // updateCourse — PATCH /staff/courses/{courseId}
    // =========================================================================

    @Test
    void student_gets_403_on_update_course() throws Exception {
        mockMvc.perform(patch("/staff/courses/{courseId}", COURSE_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateCourseBody(0L))
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void staff_updates_course_returns_200() throws Exception {
        CourseView view = aCourseView(COURSE_ID, PLAN_ID, PROGRAM_ID, "Cálculo Diferencial", 2L);

        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(updateCourseUseCase.updateCourse(any())).thenReturn(view);

        mockMvc.perform(patch("/staff/courses/{courseId}", COURSE_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateCourseBody(1L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Cálculo Diferencial"))
            .andExpect(jsonPath("$.version").value(2L));
    }

    /**
     * DEBT-F6-001 load-bearing: $.code=OPTIMISTIC_LOCK_CONFLICT proves CurriculumExceptionHandler
     * wins for OptimisticLockingFailureException from updateCourse.
     */
    @Test
    void stale_version_returns_409_with_lock_conflict_code_on_update_course() throws Exception {
        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(updateCourseUseCase.updateCourse(any()))
            .thenThrow(new OptimisticLockingFailureException("stale version"));

        mockMvc.perform(patch("/staff/courses/{courseId}", COURSE_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateCourseBody(2L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isConflict())
            // DEBT-F6-001: load-bearing assertion
            .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
    }

    @Test
    void course_not_found_returns_404_on_update() throws Exception {
        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(updateCourseUseCase.updateCourse(any()))
            .thenThrow(new CourseNotFoundException(new CourseId(COURSE_ID)));

        mockMvc.perform(patch("/staff/courses/{courseId}", COURSE_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateCourseBody(0L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isNotFound());
    }

    // =========================================================================
    // archiveCourse — POST /staff/courses/{courseId}/archive
    // =========================================================================

    @Test
    void student_gets_403_on_archive_course() throws Exception {
        mockMvc.perform(post("/staff/courses/{courseId}/archive", COURSE_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(versionedActionBody(1L))
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void staff_archives_course_returns_200() throws Exception {
        CourseView archived = aCourseViewWithStatus(COURSE_ID, PLAN_ID, PROGRAM_ID, "Física I", "ARCHIVED", 2L);

        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(archiveCourseUseCase.archiveCourse(any())).thenReturn(archived);

        mockMvc.perform(post("/staff/courses/{courseId}/archive", COURSE_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(versionedActionBody(1L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void archive_course_not_found_returns_404() throws Exception {
        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(archiveCourseUseCase.archiveCourse(any()))
            .thenThrow(new CourseNotFoundException(new CourseId(COURSE_ID)));

        mockMvc.perform(post("/staff/courses/{courseId}/archive", COURSE_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(versionedActionBody(0L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isNotFound());
    }

    // =========================================================================
    // createCourseRequirement — POST /staff/courses/{courseId}/requirements
    // =========================================================================

    @Test
    void student_gets_403_on_create_requirement() throws Exception {
        mockMvc.perform(post("/staff/courses/{courseId}/requirements", COURSE_ID)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequirementBody())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void staff_creates_requirement_returns_201_with_location() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        CourseView courseView = aCourseView(COURSE_ID, PLAN_ID, PROGRAM_ID, "Cálculo II", 1L);
        CourseRequirementView reqView = aRequirementView(UUID.randomUUID(), COURSE_ID);

        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(getCourseQuery.getCourse(COURSE_ID)).thenReturn(courseView);
        when(createCourseRequirementUseCase.createCourseRequirement(any())).thenReturn(reqView);

        mockMvc.perform(post("/staff/courses/{courseId}/requirements", COURSE_ID)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequirementBody())
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            // Load-bearing: proves version field is present in create requirement response
            .andExpect(jsonPath("$.version").value(1L));
    }

    /**
     * DEBT-F6-001 load-bearing: $.code=PREREQUISITE_CYCLE proves CurriculumExceptionHandler
     * wins for PrerequisiteCycleException thrown from createCourseRequirement.
     * Also covers IMP-007 cycle detection at the HTTP layer.
     */
    @Test
    void cycle_detected_returns_422_with_prerequisite_cycle_code() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        CourseView courseView = aCourseView(COURSE_ID, PLAN_ID, PROGRAM_ID, "Cálculo I", 1L);

        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(getCourseQuery.getCourse(COURSE_ID)).thenReturn(courseView);
        when(createCourseRequirementUseCase.createCourseRequirement(any()))
            .thenThrow(new PrerequisiteCycleException("Cycle detected"));

        mockMvc.perform(post("/staff/courses/{courseId}/requirements", COURSE_ID)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequirementBody())
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isUnprocessableEntity())
            // DEBT-F6-001 + IMP-007: load-bearing assertion for cycle detection HTTP mapping
            .andExpect(jsonPath("$.code").value("PREREQUISITE_CYCLE"));
    }

    @Test
    void idempotency_conflict_returns_409_on_create_requirement() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        CourseView courseView = aCourseView(COURSE_ID, PLAN_ID, PROGRAM_ID, "Cálculo I", 1L);

        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(getCourseQuery.getCourse(COURSE_ID)).thenReturn(courseView);
        when(createCourseRequirementUseCase.createCourseRequirement(any()))
            .thenThrow(new IdempotencyKeyConflictException(idempotencyKey, "CREATE_COURSE_REQUIREMENT"));

        mockMvc.perform(post("/staff/courses/{courseId}/requirements", COURSE_ID)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequirementBody())
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
    }

    // =========================================================================
    // updateCourseRequirement — PATCH /staff/requirements/{requirementId}
    // =========================================================================

    @Test
    void student_gets_403_on_update_requirement() throws Exception {
        mockMvc.perform(patch("/staff/requirements/{requirementId}", REQUIREMENT_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequirementBody(0L))
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void staff_updates_requirement_returns_200() throws Exception {
        CourseRequirementView reqView = aRequirementView(REQUIREMENT_ID, COURSE_ID);

        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(updateCourseRequirementUseCase.updateCourseRequirement(any())).thenReturn(reqView);

        mockMvc.perform(patch("/staff/requirements/{requirementId}", REQUIREMENT_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequirementBody(1L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("COURSE_PASSED"))
            // Load-bearing: proves toCourseRequirementDto sets version from view; removing the fix causes null
            .andExpect(jsonPath("$.version").value(1L));
    }

    /**
     * DEBT-F6-001 load-bearing: $.code=OPTIMISTIC_LOCK_CONFLICT for updateCourseRequirement.
     */
    @Test
    void stale_version_returns_409_with_lock_conflict_code_on_update_requirement() throws Exception {
        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(updateCourseRequirementUseCase.updateCourseRequirement(any()))
            .thenThrow(new OptimisticLockingFailureException("stale version"));

        mockMvc.perform(patch("/staff/requirements/{requirementId}", REQUIREMENT_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequirementBody(2L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
    }

    /**
     * FIX 2 load-bearing: cycle introduced via update path → 422 + $.code=PREREQUISITE_CYCLE.
     * If cycle detection is removed from UpdateCourseRequirementService, the mock never throws
     * and this assertion fails.
     */
    @Test
    void update_introducing_cycle_returns_422_with_prerequisite_cycle_code() throws Exception {
        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(updateCourseRequirementUseCase.updateCourseRequirement(any()))
            .thenThrow(new PrerequisiteCycleException("Cycle detected via update"));

        mockMvc.perform(patch("/staff/requirements/{requirementId}", REQUIREMENT_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequirementBody(1L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isUnprocessableEntity())
            // Load-bearing: proves CurriculumExceptionHandler maps PrerequisiteCycleException to 422
            // with $.code=PREREQUISITE_CYCLE on the update path (FIX 2 — IMP-007 gap)
            .andExpect(jsonPath("$.code").value("PREREQUISITE_CYCLE"));
    }

    @Test
    void requirement_not_found_returns_404_on_update() throws Exception {
        when(getCurrentUserQuery.getCurrentUserBySubject(any()))
            .thenReturn(aStaffUserProfile());
        when(updateCourseRequirementUseCase.updateCourseRequirement(any()))
            .thenThrow(new CourseRequirementNotFoundException(new CourseRequirementId(REQUIREMENT_ID)));

        mockMvc.perform(patch("/staff/requirements/{requirementId}", REQUIREMENT_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateRequirementBody(0L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
            .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Test data helpers
    // =========================================================================

    private UserProfile aStaffUserProfile() {
        return new UserProfile(ACTOR_ID, "staff-sub", "staff@test.com", "Staff User", "STAFF", "ACTIVE", true);
    }

    private UserProfile anAdminUserProfile() {
        return new UserProfile(ACTOR_ID, "admin-sub", "admin@test.com", "Admin User", "ADMIN", "ACTIVE", true);
    }

    private StudyPlanView aStudyPlanView(UUID planId, UUID programId) {
        return new StudyPlanView(planId, programId, "Plan Test", "ACTIVE", 160, null, 0L);
    }

    private CourseView aCourseView(UUID courseId, UUID planId, UUID programId, String name, long version) {
        return new CourseView(courseId, planId, programId, null, name, "REQUIRED", 4, 4,
            Collections.emptyList(), "INACTIVE", version);
    }

    private CourseView aCourseViewWithStatus(UUID courseId, UUID planId, UUID programId, String name, String status, long version) {
        return new CourseView(courseId, planId, programId, null, name, "REQUIRED", 4, 4,
            Collections.emptyList(), status, version);
    }

    private CourseRequirementView aRequirementView(UUID requirementId, UUID targetCourseId) {
        return new CourseRequirementView(requirementId, targetCourseId, "COURSE_PASSED",
            REQUIRED_COURSE_ID, null, false, null, null, "ACTIVE", 1L);
    }

    private String createCourseBody() {
        return """
            {
              "name": "Cálculo I",
              "type": "REQUIRED",
              "credits": 4,
              "curricularWeeklyHours": 4
            }
            """;
    }

    private String updateCourseBody(long version) {
        return """
            {
              "name": "Cálculo Diferencial",
              "type": "REQUIRED",
              "credits": 4,
              "curricularWeeklyHours": 4,
              "version": %d
            }
            """.formatted(version);
    }

    private String versionedActionBody(long version) {
        return """
            {
              "version": %d
            }
            """.formatted(version);
    }

    private String createRequirementBody() {
        return """
            {
              "type": "COURSE_PASSED",
              "requiredCourseId": "%s",
              "simultaneityAllowed": false
            }
            """.formatted(REQUIRED_COURSE_ID);
    }

    private String updateRequirementBody(long version) {
        return """
            {
              "type": "COURSE_PASSED",
              "version": %d
            }
            """.formatted(version);
    }
}

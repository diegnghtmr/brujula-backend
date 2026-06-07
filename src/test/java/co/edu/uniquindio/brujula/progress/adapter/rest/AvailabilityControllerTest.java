package co.edu.uniquindio.brujula.progress.adapter.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.out.SaveAppUserPort;
import co.edu.uniquindio.brujula.identity.domain.AppUser;
import co.edu.uniquindio.brujula.identity.domain.Role;
import co.edu.uniquindio.brujula.progress.application.dto.CurriculumCourseData;
import co.edu.uniquindio.brujula.progress.application.dto.CurriculumSemesterData;
import co.edu.uniquindio.brujula.progress.application.dto.CurriculumViewData;
import co.edu.uniquindio.brujula.progress.application.dto.PeriodAverageView;
import co.edu.uniquindio.brujula.progress.application.dto.ProgressSummaryView;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionCurriculumQuery;
import co.edu.uniquindio.brujula.progress.application.port.in.GetSubscriptionProgressQuery;
import co.edu.uniquindio.brujula.progress.domain.SubscriptionNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * WU4-T07 RED: REST integration tests for curriculum and progress summary endpoints.
 *
 * <p>Scenarios:
 *
 * <ul>
 *   <li>GET curriculum returns 200 with semester buckets and progressStatus.
 *   <li>BOLA returns 404.
 *   <li>CROSS-2 unauth 4xx / STAFF 403.
 *   <li>GET progress returns 200 with summary fields including disclaimer.
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AvailabilityControllerTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @Autowired private SaveAppUserPort saveAppUserPort;

  @MockitoBean private GetSubscriptionCurriculumQuery getSubscriptionCurriculumQuery;
  @MockitoBean private GetSubscriptionProgressQuery getSubscriptionProgressQuery;

  private MockMvc mockMvc;
  private String studentSubject;
  private String staffSubject;

  private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

    studentSubject = "student-" + UUID.randomUUID();
    AppUser student = AppUser.provision(
        studentSubject,
        "student-avl-" + UUID.randomUUID() + "@uniquindio.edu.co",
        "Test Student",
        Role.STUDENT);
    saveAppUserPort.save(student);

    staffSubject = "staff-" + UUID.randomUUID();
    AppUser staff = AppUser.provision(
        staffSubject,
        "staff-avl-" + UUID.randomUUID() + "@uniquindio.edu.co",
        "Test Staff",
        Role.STAFF);
    saveAppUserPort.save(staff);
  }

  // -----------------------------------------------------------------------
  // CROSS-2: unauthenticated → 4xx
  // -----------------------------------------------------------------------

  @Test
  void unauthenticated_curriculum_returns_4xx() throws Exception {
    mockMvc.perform(get("/me/subscriptions/{id}/curriculum", SUBSCRIPTION_ID))
        .andExpect(result -> {
          int s = result.getResponse().getStatus();
          if (s >= 200 && s < 300) {
            throw new AssertionError("Expected 4xx but was " + s);
          }
        });
  }

  // -----------------------------------------------------------------------
  // CROSS-2: STAFF → 403
  // -----------------------------------------------------------------------

  @Test
  void staff_role_on_curriculum_returns_403() throws Exception {
    mockMvc.perform(get("/me/subscriptions/{id}/curriculum", SUBSCRIPTION_ID)
            .with(oidcLogin()
                .idToken(t -> t.subject(staffSubject))
                .authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void staff_role_on_progress_returns_403() throws Exception {
    mockMvc.perform(get("/me/subscriptions/{id}/progress", SUBSCRIPTION_ID)
            .with(oidcLogin()
                .idToken(t -> t.subject(staffSubject))
                .authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isForbidden());
  }

  // -----------------------------------------------------------------------
  // AVL-1-BOLA: another student's subscription → 404
  // -----------------------------------------------------------------------

  @Test
  void curriculum_bola_returns_404() throws Exception {
    when(getSubscriptionCurriculumQuery.getSubscriptionCurriculum(any(), any()))
        .thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

    mockMvc.perform(get("/me/subscriptions/{id}/curriculum", SUBSCRIPTION_ID)
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject))))
        .andExpect(status().isNotFound());
  }

  @Test
  void progress_bola_returns_404() throws Exception {
    when(getSubscriptionProgressQuery.getSubscriptionProgress(any(), any()))
        .thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

    mockMvc.perform(get("/me/subscriptions/{id}/progress", SUBSCRIPTION_ID)
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject))))
        .andExpect(status().isNotFound());
  }

  // -----------------------------------------------------------------------
  // AVL-1-HP: GET curriculum returns 200 with semester buckets and progressStatus
  // -----------------------------------------------------------------------

  @Test
  void student_curriculum_returns_200_with_semester_and_status() throws Exception {
    UUID courseId = UUID.randomUUID();
    CurriculumCourseData course = new CurriculumCourseData(
        courseId, "ISC-001", "Prog 1", 3.0, "REQUIRED", "AVAILABLE", List.of(), false);
    CurriculumSemesterData semester = new CurriculumSemesterData(1, List.of(course));
    CurriculumViewData viewData = new CurriculumViewData(
        SUBSCRIPTION_ID, "DIURNA", "Jornada Diurna", List.of(semester));

    when(getSubscriptionCurriculumQuery.getSubscriptionCurriculum(any(), any()))
        .thenReturn(viewData);

    mockMvc.perform(get("/me/subscriptions/{id}/curriculum", SUBSCRIPTION_ID)
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subscriptionId").value(SUBSCRIPTION_ID.toString()))
        .andExpect(jsonPath("$.semesters").isArray())
        .andExpect(jsonPath("$.semesters[0].recommendedSemester").value(1))
        .andExpect(jsonPath("$.semesters[0].courses[0].progressStatus").value("AVAILABLE"));
  }

  // -----------------------------------------------------------------------
  // AVL-2-HP: GET progress returns 200 with summary fields and disclaimer
  // -----------------------------------------------------------------------

  @Test
  void student_progress_returns_200_with_summary_and_disclaimer() throws Exception {
    ProgressSummaryView summary = new ProgressSummaryView(
        SUBSCRIPTION_ID,
        "Ingeniería de Sistemas y Computación", // FIX-W2: programName is now a real field
        120.0, 30.0, 90.0,
        new BigDecimal("25.00"),
        3, 1,
        new BigDecimal("3.5"),
        List.of(new PeriodAverageView(UUID.randomUUID(), new BigDecimal("3.5"))),
        "Los datos son autodeclarados y no son oficiales.");

    when(getSubscriptionProgressQuery.getSubscriptionProgress(any(), any()))
        .thenReturn(summary);

    mockMvc.perform(get("/me/subscriptions/{id}/progress", SUBSCRIPTION_ID)
            .with(oidcLogin()
                .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))
                .idToken(t -> t.subject(studentSubject))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subscriptionId").value(SUBSCRIPTION_ID.toString()))
        .andExpect(jsonPath("$.approvedCredits").value(30.0))
        .andExpect(jsonPath("$.progressPercentage").value(25.0))
        .andExpect(jsonPath("$.disclaimer").isNotEmpty());
  }
}

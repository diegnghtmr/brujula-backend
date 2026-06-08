package co.edu.uniquindio.brujula.offering.adapter.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.dto.UserProfile;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.dto.ScheduleOptionView;
import co.edu.uniquindio.brujula.offering.application.port.in.CreateOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.GetOfferingQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListOfferingsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.PublishOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.UpdateOfferingUseCase;
import co.edu.uniquindio.brujula.offering.domain.IllegalOfferingTransitionException;
import co.edu.uniquindio.brujula.offering.domain.CourseOfferingId;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotPublishableException;
import co.edu.uniquindio.brujula.offering.domain.OfferingStatus;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;

import java.util.Collections;
import java.util.List;
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
 * WU3-TDD: Controller slice security and exception-mapping tests for publishOffering.
 *
 * <p>Tests:
 * <ul>
 *   <li>STUDENT → 403 (role guard)
 *   <li>Illegal transition → 409 with {@code $.code=ILLEGAL_OFFERING_TRANSITION}
 *   <li>No schedule options → 422
 *   <li>Stale version → 409 with {@code $.code=OPTIMISTIC_LOCK_CONFLICT}
 *   <li>Idempotency replay → 200 with stored view
 *   <li>Intensity mismatch advisory → 200 with {@code intensityWarning=true}
 *   <li>Successful publish DRAFT→ARCHIVED with no options → 200
 *   <li>Offering not found → 404
 * </ul>
 *
 * <p>Satisfies: OFFW-005, OFFW-008, WU3 Phase 8.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OfferingPublishControllerSliceTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  @MockitoBean private ListPeriodsQuery listPeriodsQuery;
  @MockitoBean private ListOfferingsQuery listOfferingsQuery;
  @MockitoBean private GetOfferingQuery getOfferingQuery;
  @MockitoBean private CreateOfferingUseCase createOfferingUseCase;
  @MockitoBean private UpdateOfferingUseCase updateOfferingUseCase;
  @MockitoBean private PublishOfferingUseCase publishOfferingUseCase;
  @MockitoBean private GetCurrentUserQuery getCurrentUserQuery;

  private MockMvc mockMvc;

  private static final UUID OFFERING_ID = UUID.randomUUID();
  private static final UUID COURSE_ID = UUID.randomUUID();
  private static final UUID PROGRAM_ID = UUID.randomUUID();
  private static final UUID PERIOD_ID = UUID.randomUUID();
  private static final UUID ACTOR_ID = UUID.randomUUID();
  private static final String XSRF_TOKEN = "test-xsrf-token";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  // ---- STUDENT → 403 ----

  @Test
  void student_gets_403_on_publish_offering() throws Exception {
    mockMvc
        .perform(
            post("/staff/offerings/{offeringId}/publication", OFFERING_ID)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", "any-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("PRELIMINARY", 0L))
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  // ---- Illegal transition → 409 ----

  @Test
  void illegal_transition_returns_409_with_code() throws Exception {
    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(new UserProfile(ACTOR_ID, "staff-sub", "staff@test.com", "Staff", "STAFF", "ACTIVE", true));
    CourseOfferingId offeringId = CourseOfferingId.of(OFFERING_ID);
    when(publishOfferingUseCase.publishOffering(any()))
        .thenThrow(new IllegalOfferingTransitionException(
            offeringId, OfferingStatus.ARCHIVED, OfferingStatus.PRELIMINARY));

    mockMvc
        .perform(
            post("/staff/offerings/{offeringId}/publication", OFFERING_ID)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("PRELIMINARY", 1L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ILLEGAL_OFFERING_TRANSITION"));
  }

  // ---- No schedule options → 422 ----

  @Test
  void no_schedule_options_returns_422() throws Exception {
    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(new UserProfile(ACTOR_ID, "staff-sub", "staff@test.com", "Staff", "STAFF", "ACTIVE", true));
    CourseOfferingId offeringId = CourseOfferingId.of(OFFERING_ID);
    when(publishOfferingUseCase.publishOffering(any()))
        .thenThrow(new OfferingNotPublishableException(offeringId));

    mockMvc
        .perform(
            post("/staff/offerings/{offeringId}/publication", OFFERING_ID)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("PRELIMINARY", 0L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isUnprocessableEntity());
  }

  // ---- Stale version → 409 with code ----

  @Test
  void stale_version_returns_409_with_lock_conflict_code() throws Exception {
    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(new UserProfile(ACTOR_ID, "staff-sub", "staff@test.com", "Staff", "STAFF", "ACTIVE", true));
    when(publishOfferingUseCase.publishOffering(any()))
        .thenThrow(new OptimisticLockingFailureException("stale"));

    mockMvc
        .perform(
            post("/staff/offerings/{offeringId}/publication", OFFERING_ID)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("PRELIMINARY", 1L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
  }

  // ---- Idempotency replay → 200 ----

  @Test
  void idempotency_replay_returns_200_with_stored_view() throws Exception {
    OfferingDetailView view = sampleView("PRELIMINARY", 2L, null);
    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(new UserProfile(ACTOR_ID, "staff-sub", "staff@test.com", "Staff", "STAFF", "ACTIVE", true));
    when(publishOfferingUseCase.publishOffering(any())).thenReturn(view);

    mockMvc
        .perform(
            post("/staff/offerings/{offeringId}/publication", OFFERING_ID)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("PRELIMINARY", 1L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PRELIMINARY"))
        .andExpect(jsonPath("$.version").value(2));
  }

  // ---- Intensity mismatch advisory → 200 + intensityWarning=true ----

  @Test
  void intensity_mismatch_returns_200_with_intensity_warning_true() throws Exception {
    OfferingDetailView view = sampleView("PRELIMINARY", 1L, true);
    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(new UserProfile(ACTOR_ID, "staff-sub", "staff@test.com", "Staff", "STAFF", "ACTIVE", true));
    when(publishOfferingUseCase.publishOffering(any())).thenReturn(view);

    mockMvc
        .perform(
            post("/staff/offerings/{offeringId}/publication", OFFERING_ID)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("PRELIMINARY", 0L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.intensityWarning").value(true));
  }

  // ---- DRAFT → ARCHIVED with no options → 200 (archive doesn't require options) ----

  @Test
  void archive_without_options_returns_200() throws Exception {
    OfferingDetailView view = sampleView("ARCHIVED", 1L, null);
    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(new UserProfile(ACTOR_ID, "staff-sub", "staff@test.com", "Staff", "STAFF", "ACTIVE", true));
    when(publishOfferingUseCase.publishOffering(any())).thenReturn(view);

    mockMvc
        .perform(
            post("/staff/offerings/{offeringId}/publication", OFFERING_ID)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("ARCHIVED", 0L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"));
  }

  // ---- Offering not found → 404 ----

  @Test
  void offering_not_found_returns_404() throws Exception {
    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(new UserProfile(ACTOR_ID, "staff-sub", "staff@test.com", "Staff", "STAFF", "ACTIVE", true));
    CourseOfferingId offeringId = CourseOfferingId.of(OFFERING_ID);
    when(publishOfferingUseCase.publishOffering(any()))
        .thenThrow(new OfferingNotFoundException(offeringId));

    mockMvc
        .perform(
            post("/staff/offerings/{offeringId}/publication", OFFERING_ID)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(publishBody("PRELIMINARY", 0L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isNotFound());
  }

  // ---- Helpers ----

  private String publishBody(String targetStatus, long version) {
    return """
        {
          "targetStatus": "%s",
          "version": %d
        }
        """.formatted(targetStatus, version);
  }

  private OfferingDetailView sampleView(String status, long version, Boolean intensityWarning) {
    List<ScheduleOptionView> options = Collections.emptyList();
    return new OfferingDetailView(
        OFFERING_ID, COURSE_ID, "Course Name", PROGRAM_ID, PERIOD_ID,
        status, null, version, options, intensityWarning);
  }
}

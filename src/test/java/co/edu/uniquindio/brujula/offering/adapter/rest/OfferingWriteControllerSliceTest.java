package co.edu.uniquindio.brujula.offering.adapter.rest;

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
import co.edu.uniquindio.brujula.identity.application.dto.UserProfile;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingDetailView;
import co.edu.uniquindio.brujula.offering.application.port.in.CreateOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.GetOfferingQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListOfferingsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.UpdateOfferingUseCase;
import co.edu.uniquindio.brujula.offering.domain.OfferingNotFoundException;
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
 * Controller slice security and exception-mapping tests for offering write endpoints.
 *
 * <p>Tests:
 *
 * <ul>
 *   <li>POST /staff/programs/{id}/offerings — STUDENT → 403; STAFF → 201 + Location; idempotency
 *       conflict → 409
 *   <li>PATCH /staff/offerings/{id} — STUDENT → 403; stale version → 409 + {@code
 *       $.code=OPTIMISTIC_LOCK_CONFLICT}; success → 200
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OfferingWriteControllerSliceTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  @MockitoBean private ListPeriodsQuery listPeriodsQuery;
  @MockitoBean private ListOfferingsQuery listOfferingsQuery;
  @MockitoBean private GetOfferingQuery getOfferingQuery;
  @MockitoBean private CreateOfferingUseCase createOfferingUseCase;
  @MockitoBean private UpdateOfferingUseCase updateOfferingUseCase;
  @MockitoBean private GetCurrentUserQuery getCurrentUserQuery;

  private MockMvc mockMvc;

  private static final UUID PROGRAM_ID = UUID.randomUUID();
  private static final UUID OFFERING_ID = UUID.randomUUID();
  private static final UUID COURSE_ID = UUID.randomUUID();
  private static final UUID PERIOD_ID = UUID.randomUUID();
  private static final UUID ACTOR_ID = UUID.randomUUID();
  private static final String XSRF_TOKEN = "test-xsrf-token";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  // ---- createOffering ----

  @Test
  void student_gets_403_on_create_offering() throws Exception {
    mockMvc
        .perform(
            post("/staff/programs/{programId}/offerings", PROGRAM_ID)
                .header("Idempotency-Key", UUID.randomUUID())
                .header("X-XSRF-TOKEN", "test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createOfferingBody())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void staff_gets_201_on_create_offering() throws Exception {
    UUID idempotencyKey = UUID.randomUUID();
    OfferingDetailView view =
        new OfferingDetailView(
            OFFERING_ID,
            COURSE_ID,
            "Programación I",
            PROGRAM_ID,
            PERIOD_ID,
            "DRAFT",
            null,
            0L,
            Collections.emptyList());

    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(
            new UserProfile(
                ACTOR_ID, "staff-sub", "staff@test.com", "Staff User", "STAFF", "ACTIVE", true));
    when(createOfferingUseCase.createOffering(any())).thenReturn(view);

    mockMvc
        .perform(
            post("/staff/programs/{programId}/offerings", PROGRAM_ID)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createOfferingBody())
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.version").value(0));
  }

  @Test
  void idempotency_conflict_returns_409_on_create_offering() throws Exception {
    UUID idempotencyKey = UUID.randomUUID();

    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(
            new UserProfile(
                ACTOR_ID, "staff-sub", "staff@test.com", "Staff", "STAFF", "ACTIVE", true));
    when(createOfferingUseCase.createOffering(any()))
        .thenThrow(new IdempotencyKeyConflictException(idempotencyKey, "CREATE_OFFERING"));

    mockMvc
        .perform(
            post("/staff/programs/{programId}/offerings", PROGRAM_ID)
                .header("Idempotency-Key", idempotencyKey)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createOfferingBody())
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isConflict());
  }

  // ---- updateOffering ----

  @Test
  void student_gets_403_on_update_offering() throws Exception {
    mockMvc
        .perform(
            patch("/staff/offerings/{offeringId}", OFFERING_ID)
                .header("X-XSRF-TOKEN", "test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateOfferingBody(0L))
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void stale_version_returns_409_with_lock_conflict_code_on_update_offering() throws Exception {
    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(
            new UserProfile(
                ACTOR_ID, "staff-sub", "staff@test.com", "Staff", "STAFF", "ACTIVE", true));
    when(updateOfferingUseCase.updateOffering(any()))
        .thenThrow(new OptimisticLockingFailureException("stale version"));

    mockMvc
        .perform(
            patch("/staff/offerings/{offeringId}", OFFERING_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateOfferingBody(2L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
  }

  @Test
  void missing_offering_returns_404_on_update() throws Exception {
    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(
            new UserProfile(
                ACTOR_ID, "staff-sub", "staff@test.com", "Staff", "STAFF", "ACTIVE", true));
    when(updateOfferingUseCase.updateOffering(any()))
        .thenThrow(new OfferingNotFoundException(OFFERING_ID));

    mockMvc
        .perform(
            patch("/staff/offerings/{offeringId}", OFFERING_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateOfferingBody(0L))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void staff_gets_200_with_incremented_version_on_update() throws Exception {
    long inputVersion = 3L;
    long bumpedVersion = 4L;
    OfferingDetailView view =
        new OfferingDetailView(
            OFFERING_ID,
            COURSE_ID,
            "Programación I",
            PROGRAM_ID,
            PERIOD_ID,
            "DRAFT",
            "Updated notes",
            bumpedVersion,
            Collections.emptyList());

    when(getCurrentUserQuery.getCurrentUserBySubject(any()))
        .thenReturn(
            new UserProfile(
                ACTOR_ID, "staff-sub", "staff@test.com", "Staff", "STAFF", "ACTIVE", true));
    when(updateOfferingUseCase.updateOffering(any())).thenReturn(view);

    mockMvc
        .perform(
            patch("/staff/offerings/{offeringId}", OFFERING_ID)
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateOfferingBody(inputVersion))
                .with(csrf())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(bumpedVersion));
  }

  // ---- Helpers ----

  private String createOfferingBody() {
    return """
        {
          "courseId": "%s",
          "periodId": "%s"
        }
        """
        .formatted(COURSE_ID, PERIOD_ID);
  }

  private String updateOfferingBody(long version) {
    return """
        {
          "notes": "Updated notes",
          "version": %d
        }
        """
        .formatted(version);
  }
}

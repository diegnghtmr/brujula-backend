package co.edu.uniquindio.brujula.administration.adapter.rest;

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
import co.edu.uniquindio.brujula.offering.application.dto.AcademicPeriodView;
import co.edu.uniquindio.brujula.offering.application.port.in.CreatePeriodUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.UpdatePeriodUseCase;
import co.edu.uniquindio.brujula.offering.domain.PeriodNotFoundException;
import co.edu.uniquindio.brujula.sharedkernel.application.exception.IdempotencyKeyConflictException;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Controller slice security and exception-mapping tests for period CRUD endpoints.
 *
 * <p>Verifies: ADMIN 201 / STAFF 403 for createPeriod; ADMIN 200 / STAFF 403 for updatePeriod;
 * idempotency-conflict 409; name-dupe 409; stale-version 409; period-not-found 404.
 *
 * <p>Satisfies: OFFW-001, OFFW-002, OFFW-008 (phase 7, WU1).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AdministrationPeriodControllerSliceTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;
  @MockitoBean private CreatePeriodUseCase createPeriodUseCase;
  @MockitoBean private UpdatePeriodUseCase updatePeriodUseCase;
  @MockitoBean private GetCurrentUserQuery getCurrentUserQuery;

  private MockMvc mockMvc;

  private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
  private static final String CREATE_PERIOD_URL = "/admin/periods";
  private static final String UPDATE_PERIOD_URL = "/admin/periods/{id}";
  private static final String XSRF_TOKEN = "test-xsrf-token";
  private static final String VALID_CREATE_BODY =
      "{\"name\":\"2026-3\",\"startDate\":\"2026-07-01\",\"endDate\":\"2026-12-15\",\"status\":\"UPCOMING\"}";
  private static final String VALID_UPDATE_BODY = "{\"status\":\"ACTIVE\",\"version\":1}";

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    // Stub the actor resolution used by resolveActorId() inside the controller
    when(getCurrentUserQuery.getCurrentUserBySubject(any(String.class)))
        .thenReturn(
            new UserProfile(
                ACTOR_ID, "sub-admin", "admin@test.com", "Admin", "ADMIN", "ACTIVE", true));
  }

  // -------------------------------------------------------------------------
  // POST /admin/periods — createPeriod
  // -------------------------------------------------------------------------

  @Test
  void staff_gets_403_on_create_period() throws Exception {
    mockMvc
        .perform(
            post(CREATE_PERIOD_URL)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF")))
                .with(csrf())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CREATE_BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void student_gets_403_on_create_period() throws Exception {
    mockMvc
        .perform(
            post(CREATE_PERIOD_URL)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                .with(csrf())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CREATE_BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void admin_gets_201_on_create_period() throws Exception {
    UUID periodId = UUID.randomUUID();
    AcademicPeriodView view =
        new AcademicPeriodView(
            periodId,
            "2026-3",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 12, 15),
            "UPCOMING",
            0L);
    when(createPeriodUseCase.createPeriod(any())).thenReturn(view);

    mockMvc
        .perform(
            post(CREATE_PERIOD_URL)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .with(csrf())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CREATE_BODY))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/admin/periods/" + periodId))
        .andExpect(jsonPath("$.id").value(periodId.toString()))
        .andExpect(jsonPath("$.name").value("2026-3"));
  }

  @Test
  void idempotency_conflict_returns_409() throws Exception {
    when(createPeriodUseCase.createPeriod(any()))
        .thenThrow(new IdempotencyKeyConflictException(UUID.randomUUID(), "CREATE_PERIOD"));

    mockMvc
        .perform(
            post(CREATE_PERIOD_URL)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .with(csrf())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CREATE_BODY))
        .andExpect(status().isConflict());
  }

  @Test
  void period_name_duplicate_returns_409() throws Exception {
    when(createPeriodUseCase.createPeriod(any()))
        .thenThrow(
            new DataIntegrityViolationException("duplicate key value violates unique constraint"));

    mockMvc
        .perform(
            post(CREATE_PERIOD_URL)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .with(csrf())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CREATE_BODY))
        .andExpect(status().isConflict());
  }

  // -------------------------------------------------------------------------
  // PATCH /admin/periods/{id} — updatePeriod
  // -------------------------------------------------------------------------

  @Test
  void staff_gets_403_on_update_period() throws Exception {
    mockMvc
        .perform(
            patch(UPDATE_PERIOD_URL, UUID.randomUUID())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF")))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_UPDATE_BODY))
        .andExpect(status().isForbidden());
  }

  @Test
  void admin_gets_200_on_update_period() throws Exception {
    UUID periodId = UUID.randomUUID();
    AcademicPeriodView view =
        new AcademicPeriodView(
            periodId, "2026-3", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 15), "ACTIVE", 2L);
    when(updatePeriodUseCase.updatePeriod(any())).thenReturn(view);

    mockMvc
        .perform(
            patch(UPDATE_PERIOD_URL, periodId)
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .with(csrf())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_UPDATE_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.version").value(2));
  }

  @Test
  void stale_version_returns_409() throws Exception {
    when(updatePeriodUseCase.updatePeriod(any()))
        .thenThrow(new OptimisticLockingFailureException("stale version"));

    mockMvc
        .perform(
            patch(UPDATE_PERIOD_URL, UUID.randomUUID())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .with(csrf())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_UPDATE_BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("OPTIMISTIC_LOCK_CONFLICT"));
  }

  @Test
  void period_not_found_returns_404() throws Exception {
    when(updatePeriodUseCase.updatePeriod(any()))
        .thenThrow(new PeriodNotFoundException("period not found: " + UUID.randomUUID()));

    mockMvc
        .perform(
            patch(UPDATE_PERIOD_URL, UUID.randomUUID())
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .with(csrf())
                .header("X-XSRF-TOKEN", XSRF_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_UPDATE_BODY))
        .andExpect(status().isNotFound());
  }
}

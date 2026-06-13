package co.edu.uniquindio.brujula.programcatalog.adapter.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetProgramQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.GetStudyPlanQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListJourneysQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListProgramPlansQuery;
import co.edu.uniquindio.brujula.programcatalog.application.port.in.ListProgramsQuery;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramId;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramNotFoundException;
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
 * Regression scope tests for {@code ProgramCatalogExceptionHandler}.
 *
 * <p>Asserts the per-module response contract for module-specific exceptions (status code). {@code
 * basePackages} scoping is defence-in-depth; {@code @Order(HIGHEST_PRECEDENCE)} is the primary
 * correctness guarantee. These tests catch contract regressions (wrong status or handler not
 * firing), not {@code basePackages} removal while handlers remain behaviorally identical.
 *
 * <p>Note: the programcatalog module does not handle any sharedkernel shared exceptions
 * (idempotency, optimistic-lock); its exception contract is 404-only for module-specific not-found
 * exceptions. Status assertion is the load-bearing check for this handler.
 *
 * <p>Verifies catalog controller → {@code ProgramCatalogExceptionHandler} maps {@link
 * ProgramNotFoundException} to HTTP 404.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ProgramCatalogExceptionHandlerScopeTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  @MockitoBean private ListProgramsQuery listProgramsQuery;
  @MockitoBean private GetProgramQuery getProgramQuery;
  @MockitoBean private ListProgramPlansQuery listProgramPlansQuery;
  @MockitoBean private GetStudyPlanQuery getStudyPlanQuery;
  @MockitoBean private ListJourneysQuery listJourneysQuery;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  /**
   * Catalog controller throwing {@link ProgramNotFoundException} MUST return HTTP 404 from {@code
   * ProgramCatalogExceptionHandler} (scoped to the programcatalog REST package).
   *
   * <p>This is a status-level contract test (no {@code $.code} on 404 per design). If a handler
   * regression drops the 404 mapping, this test catches it.
   */
  @Test
  void catalog_controller_program_not_found_returns_404_from_catalog_handler() throws Exception {
    UUID programId = UUID.randomUUID();
    when(getProgramQuery.getProgram(any()))
        .thenThrow(new ProgramNotFoundException(new ProgramId(programId)));

    mockMvc
        .perform(
            get("/programs/{programId}", programId)
                .with(
                    oidcLogin()
                        .idToken(t -> t.subject("scope-catalog-sub"))
                        .authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isNotFound());
  }
}

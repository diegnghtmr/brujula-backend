package co.edu.uniquindio.brujula.offering.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.identity.application.port.in.GetCurrentUserQuery;
import co.edu.uniquindio.brujula.offering.application.dto.ListOfferingsCriteria;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingPageView;
import co.edu.uniquindio.brujula.offering.application.dto.OfferingSummaryView;
import co.edu.uniquindio.brujula.offering.application.port.in.CreateOfferingUseCase;
import co.edu.uniquindio.brujula.offering.application.port.in.GetOfferingQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListOfferingsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.ListPeriodsQuery;
import co.edu.uniquindio.brujula.offering.application.port.in.UpdateOfferingUseCase;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * WU2-TDD: OFFW-007 — listOfferings DRAFT filter widening for STAFF/ADMIN.
 *
 * <p>Tests:
 * <ul>
 *   <li>STAFF can list DRAFT offerings (status=DRAFT returns non-empty page)
 *   <li>STUDENT requesting status=DRAFT gets an empty page (DRAFT filtered out — NOT 403)
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OfferingListDraftFilterSliceTest extends AbstractPostgresIntegrationTest {

  @Autowired private WebApplicationContext context;

  @MockitoBean private ListPeriodsQuery listPeriodsQuery;
  @MockitoBean private ListOfferingsQuery listOfferingsQuery;
  @MockitoBean private GetOfferingQuery getOfferingQuery;
  @MockitoBean private CreateOfferingUseCase createOfferingUseCase;
  @MockitoBean private UpdateOfferingUseCase updateOfferingUseCase;
  @MockitoBean private GetCurrentUserQuery getCurrentUserQuery;

  private MockMvc mockMvc;

  private static final UUID PERIOD_ID = UUID.randomUUID();
  private static final UUID COURSE_ID = UUID.randomUUID();
  private static final UUID PROGRAM_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void staff_can_list_draft_offerings() throws Exception {
    OfferingPageView draftPage = new OfferingPageView(
        List.of(new OfferingSummaryView(
            UUID.randomUUID(), COURSE_ID, "Álgebra", PROGRAM_ID, PERIOD_ID,
            "DRAFT", null, 0L)),
        0, 20, 1L);

    when(listOfferingsQuery.listOfferings(any(ListOfferingsCriteria.class)))
        .thenReturn(draftPage);

    mockMvc
        .perform(
            get("/offerings")
                .param("periodId", PERIOD_ID.toString())
                .param("status", "DRAFT")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STAFF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].status").value("DRAFT"));

    // OFFW-007 routing regression guard: assert criteria carries STAFF as privileged caller.
    // A bug that always called the student path would still return 200 but this capture fails.
    ArgumentCaptor<ListOfferingsCriteria> captor = forClass(ListOfferingsCriteria.class);
    verify(listOfferingsQuery).listOfferings(captor.capture());
    ListOfferingsCriteria captured = captor.getValue();
    assertThat(captured.callerRole()).isEqualTo("STAFF");
    assertThat(captured.isPrivilegedCaller()).isTrue();
  }

  @Test
  void student_requesting_draft_gets_empty_page_not_403() throws Exception {
    // Service returns empty page for STUDENT + DRAFT filter
    when(listOfferingsQuery.listOfferings(any(ListOfferingsCriteria.class)))
        .thenReturn(new OfferingPageView(Collections.emptyList(), 0, 20, 0L));

    mockMvc
        .perform(
            get("/offerings")
                .param("periodId", PERIOD_ID.toString())
                .param("status", "DRAFT")
                .with(oidcLogin().authorities(new SimpleGrantedAuthority("ROLE_STUDENT"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0))
        .andExpect(jsonPath("$.content").isEmpty());

    // OFFW-007 routing regression guard: assert criteria carries STUDENT as non-privileged caller.
    // A routing regression that always called the staff path would pass the response assertions
    // above but fail here when the mock returns empty regardless.
    ArgumentCaptor<ListOfferingsCriteria> captor = forClass(ListOfferingsCriteria.class);
    verify(listOfferingsQuery).listOfferings(captor.capture());
    ListOfferingsCriteria captured = captor.getValue();
    assertThat(captured.callerRole()).isEqualTo("STUDENT");
    assertThat(captured.isPrivilegedCaller()).isFalse();
  }
}

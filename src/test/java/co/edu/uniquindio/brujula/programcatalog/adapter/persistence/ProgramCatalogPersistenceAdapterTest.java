package co.edu.uniquindio.brujula.programcatalog.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.brujula.AbstractPostgresIntegrationTest;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadJourneyPort;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadProgramPort;
import co.edu.uniquindio.brujula.programcatalog.application.port.out.LoadStudyPlanPort;
import co.edu.uniquindio.brujula.programcatalog.domain.AcademicProgram;
import co.edu.uniquindio.brujula.programcatalog.domain.Journey;
import co.edu.uniquindio.brujula.programcatalog.domain.PlanId;
import co.edu.uniquindio.brujula.programcatalog.domain.PlanStatus;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramId;
import co.edu.uniquindio.brujula.programcatalog.domain.ProgramStatus;
import co.edu.uniquindio.brujula.programcatalog.domain.StudyPlan;
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
 * RED → GREEN persistence adapter tests: catalog JPA round-trip.
 *
 * <p>Covers: program load-all + load-by-id, study plan load-by-programId + load-by-id, journey
 * load-all-active. Tests directly through the port interfaces.
 *
 * <p>Uses Testcontainers PostgreSQL + Flyway for a real schema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProgramCatalogPersistenceAdapterTest extends AbstractPostgresIntegrationTest {

  @Autowired private LoadProgramPort loadProgramPort;
  @Autowired private LoadStudyPlanPort loadStudyPlanPort;
  @Autowired private LoadJourneyPort loadJourneyPort;
  @Autowired private JdbcTemplate jdbcTemplate;
  @PersistenceContext private EntityManager entityManager;

  // ---------------------------------------------------------------------------
  // LoadProgramPort
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void load_program_by_id_returns_correct_program() {
    UUID programId = insertProgram("Ingeniería de Sistemas y Computación", "Ingeniería", "ACTIVE");

    Optional<AcademicProgram> result = loadProgramPort.loadById(new ProgramId(programId));

    assertThat(result).isPresent();
    AcademicProgram program = result.get();
    assertThat(program.id().value()).isEqualTo(programId);
    assertThat(program.name()).isEqualTo("Ingeniería de Sistemas y Computación");
    assertThat(program.faculty()).isEqualTo("Ingeniería");
    assertThat(program.status()).isEqualTo(ProgramStatus.ACTIVE);
  }

  @Test
  @Transactional
  void load_program_by_nonexistent_id_returns_empty() {
    Optional<AcademicProgram> result = loadProgramPort.loadById(new ProgramId(UUID.randomUUID()));

    assertThat(result).isEmpty();
  }

  @Test
  @Transactional
  void load_all_programs_includes_inserted_program() {
    UUID programId = insertProgram("Lenguas Modernas y Cultura", "Ciencias Humanas", "ACTIVE");

    List<AcademicProgram> all = loadProgramPort.loadAll();

    assertThat(all).extracting(p -> p.id().value()).contains(programId);
  }

  @Test
  @Transactional
  void program_version_starts_at_zero() {
    UUID programId = insertProgram("Administración de Empresas", "Ciencias Económicas", "ACTIVE");

    Optional<AcademicProgram> result = loadProgramPort.loadById(new ProgramId(programId));

    assertThat(result).isPresent();
    assertThat(result.get().version()).isZero();
  }

  // ---------------------------------------------------------------------------
  // LoadStudyPlanPort
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void load_study_plan_by_id_returns_correct_plan() {
    UUID programId = insertProgram("Ingeniería Civil", "Ingeniería", "ACTIVE");
    UUID planId = insertStudyPlan(programId, "Plan 2016", "ACTIVE", 170, 2016);

    Optional<StudyPlan> result = loadStudyPlanPort.loadById(new PlanId(planId));

    assertThat(result).isPresent();
    StudyPlan plan = result.get();
    assertThat(plan.id().value()).isEqualTo(planId);
    assertThat(plan.programId()).isEqualTo(programId);
    assertThat(plan.name()).isEqualTo("Plan 2016");
    assertThat(plan.status()).isEqualTo(PlanStatus.ACTIVE);
    assertThat(plan.totalCredits()).isEqualTo(170);
    assertThat(plan.referenceYear()).isEqualTo(2016);
  }

  @Test
  @Transactional
  void load_study_plan_by_nonexistent_id_returns_empty() {
    Optional<StudyPlan> result = loadStudyPlanPort.loadById(new PlanId(UUID.randomUUID()));

    assertThat(result).isEmpty();
  }

  @Test
  @Transactional
  void load_study_plans_by_program_id_returns_all_plans_for_that_program() {
    UUID programId = insertProgram("Medicina", "Ciencias de la Salud", "ACTIVE");
    UUID plan1Id = insertStudyPlan(programId, "Plan 2010", "ARCHIVED", 240, 2010);
    UUID plan2Id = insertStudyPlan(programId, "Plan 2022", "ACTIVE", 260, 2022);

    List<StudyPlan> plans = loadStudyPlanPort.loadByProgramId(programId);

    assertThat(plans).extracting(p -> p.id().value()).containsExactlyInAnyOrder(plan1Id, plan2Id);
  }

  @Test
  @Transactional
  void load_study_plans_by_program_id_returns_empty_for_unknown_program() {
    List<StudyPlan> plans = loadStudyPlanPort.loadByProgramId(UUID.randomUUID());

    assertThat(plans).isEmpty();
  }

  @Test
  @Transactional
  void study_plan_optimistic_lock_version_starts_at_zero() {
    UUID programId = insertProgram("Contaduría Pública", "Ciencias Económicas", "ACTIVE");
    UUID planId = insertStudyPlan(programId, "Plan 2018", "DRAFT", 150, 2018);

    Optional<StudyPlan> result = loadStudyPlanPort.loadById(new PlanId(planId));

    assertThat(result).isPresent();
    assertThat(result.get().version()).isZero();
  }

  @Test
  @Transactional
  void study_plan_with_null_reference_year_loads_correctly() {
    UUID programId = insertProgram("Matemáticas", "Ciencias Exactas", "ACTIVE");
    UUID planId = insertStudyPlanWithoutYear(programId, "Plan Base", "DRAFT", 140);

    Optional<StudyPlan> result = loadStudyPlanPort.loadById(new PlanId(planId));

    assertThat(result).isPresent();
    assertThat(result.get().referenceYear()).isNull();
  }

  // ---------------------------------------------------------------------------
  // LoadJourneyPort
  // ---------------------------------------------------------------------------

  @Test
  @Transactional
  void load_all_active_journeys_returns_only_active_journeys() {
    UUID activeId = insertJourney("NOCTURNA-" + UUID.randomUUID(), "Jornada Nocturna", true);
    UUID inactiveId = insertJourney("VIRTUAL-" + UUID.randomUUID(), "Jornada Virtual", false);

    List<Journey> active = loadJourneyPort.loadAllActive();

    List<UUID> activeIds = active.stream().map(j -> j.id().value()).toList();
    assertThat(activeIds).contains(activeId);
    assertThat(activeIds).doesNotContain(inactiveId);
  }

  @Test
  @Transactional
  void journey_fields_are_mapped_correctly() {
    String code = "ESPECIAL-" + UUID.randomUUID();
    UUID journeyId = insertJourney(code, "Jornada Especial", true);

    List<Journey> journeys = loadJourneyPort.loadAllActive();

    Journey found =
        journeys.stream().filter(j -> j.id().value().equals(journeyId)).findFirst().orElseThrow();

    assertThat(found.code()).isEqualTo(code);
    assertThat(found.name()).isEqualTo("Jornada Especial");
    assertThat(found.active()).isTrue();
  }

  // ---------------------------------------------------------------------------
  // SQL insert helpers (bypass write adapters to keep tests focused on read paths)
  // ---------------------------------------------------------------------------

  private UUID insertProgram(String name, String faculty, String status) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.academic_program (id, name, faculty, status, version) "
            + "VALUES (?, ?, ?, ?, 0)",
        id,
        name,
        faculty,
        status);
    entityManager.flush();
    return id;
  }

  private UUID insertStudyPlan(
      UUID programId, String name, String status, int credits, Integer year) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, reference_year, version) "
            + "VALUES (?, ?, ?, ?, ?, ?, 0)",
        id,
        programId,
        name,
        status,
        credits,
        year);
    entityManager.flush();
    return id;
  }

  private UUID insertStudyPlanWithoutYear(UUID programId, String name, String status, int credits) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.study_plan (id, program_id, name, status, total_credits, version) "
            + "VALUES (?, ?, ?, ?, ?, 0)",
        id,
        programId,
        name,
        status,
        credits);
    entityManager.flush();
    return id;
  }

  private UUID insertJourney(String code, String name, boolean active) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO catalog.journey (id, code, name, active) VALUES (?, ?, ?, ?)",
        id,
        code,
        name,
        active);
    entityManager.flush();
    return id;
  }
}

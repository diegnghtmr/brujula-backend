package co.edu.uniquindio.brujula.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * WU2-T04: {@code progress.domain} must have zero Spring/JPA/HTTP/generated imports.
 *
 * <p>Mirrors {@link PrerequisiteAcyclicityServicePurityTest} for the {@code progress} module.
 * Populated in WU2 once {@code progress.domain} production classes exist.
 *
 * <p>Satisfies: ARQ-002, ADR-F3-001 (progress side).
 */
@AnalyzeClasses(
    packages = "co.edu.uniquindio.brujula.progress.domain",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ProgressDomainPurityTest {

  /**
   * progress.domain must not import Spring Framework, JPA, servlet, or generated model types.
   *
   * <p>Enforces ARQ-002 for the progress module.
   */
  @ArchTest
  static final ArchRule progress_domain_must_not_import_spring_jpa_or_generated =
      noClasses()
          .that()
          .resideInAPackage("co.edu.uniquindio.brujula.progress.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "jakarta.servlet..",
              "..generated.model..",
              "..adapter..");
}

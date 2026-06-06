package co.edu.uniquindio.brujula.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * WU1-3.5 RED → GREEN: ADR-F2-006 — {@code PrerequisiteAcyclicityService} must have ZERO imports
 * from {@code org.springframework} or {@code jakarta.persistence}.
 *
 * <p>This rule enforces that the domain cycle-detection service remains framework-free and
 * unit-testable, per the hexagonal architecture invariant (ARQ-002).
 */
@AnalyzeClasses(
    packages = "co.edu.uniquindio.brujula.curriculum.domain",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class PrerequisiteAcyclicityServicePurityTest {

  /**
   * ADR-F2-006 — PrerequisiteAcyclicityService must not import Spring or Jakarta Persistence.
   */
  @ArchTest
  static final ArchRule prerequisiteAcyclicityServiceIsFrameworkFree =
      noClasses()
          .that()
          .haveSimpleName("PrerequisiteAcyclicityService")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework..", "jakarta.persistence..");
}

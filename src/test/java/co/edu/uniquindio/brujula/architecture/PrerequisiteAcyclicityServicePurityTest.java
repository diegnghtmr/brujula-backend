package co.edu.uniquindio.brujula.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * {@code PrerequisiteAcyclicityService} must have zero imports from {@code org.springframework} or
 * {@code jakarta.persistence}.
 *
 * <p>This rule enforces that the domain cycle-detection service remains framework-free and
 * unit-testable (hexagonal architecture: domain must not depend on any framework).
 */
@AnalyzeClasses(
    packages = "co.edu.uniquindio.brujula.curriculum.domain",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class PrerequisiteAcyclicityServicePurityTest {

  /** PrerequisiteAcyclicityService must not import Spring or Jakarta Persistence. */
  @ArchTest
  static final ArchRule prerequisiteAcyclicityServiceIsFrameworkFree =
      noClasses()
          .that()
          .haveSimpleName("PrerequisiteAcyclicityService")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework..", "jakarta.persistence..");
}

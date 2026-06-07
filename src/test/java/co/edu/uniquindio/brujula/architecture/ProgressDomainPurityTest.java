package co.edu.uniquindio.brujula.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import org.junit.jupiter.api.Disabled;

/**
 * WU1-T08 skeleton / WU2-T04 (to be filled): ADR-F3-001 — {@code progress.domain} must have zero
 * Spring/JPA/HTTP/generated imports.
 *
 * <p>Mirrors {@link PrerequisiteAcyclicityServicePurityTest} for the {@code progress} module.
 * Assertions will be added when the {@code progress.domain} package is populated in WU2–WU4.
 *
 * <p>This class is intentionally empty until WU2 introduces {@code progress.domain} production
 * classes. Without {@code @Disabled}, ArchUnit JUnit5 generates zero test cases and the class
 * appears as a silent, inert non-contributor to the suite — giving false confidence that purity is
 * being checked. Adding {@code @Disabled} makes the placeholder intent explicit and visible.
 *
 * <p>Satisfies: ARQ-002, ADR-F3-001 (progress side). FIX-3 (SUGGESTION-001).
 */
@Disabled("placeholder until WU2 introduces progress.domain production classes (WU2-T04)")
@AnalyzeClasses(
    packages = "co.edu.uniquindio.brujula.progress.domain",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ProgressDomainPurityTest {
  // @ArchTest fields will be added in WU2-T04 when progress.domain classes exist.
  // Skeleton kept here so WU1 can commit cleanly and WU2 picks up an existing file.
}

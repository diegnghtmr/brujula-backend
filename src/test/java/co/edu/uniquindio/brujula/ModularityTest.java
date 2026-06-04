package co.edu.uniquindio.brujula;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifies Spring Modulith module structure constraints.
 *
 * <p>Ensures no illegal cross-module dependencies exist and no dependency cycles are detected
 * across the 10 functional modules.
 */
class ModularityTest {

    static final ApplicationModules MODULES =
            ApplicationModules.of(BrujulaApplication.class);

    @Test
    void verifyModularStructure() {
        MODULES.verify();
    }
}

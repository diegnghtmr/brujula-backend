/**
 * Application-layer DTOs for the programcatalog module.
 *
 * <p>Named interface {@code api-dto}: exposed alongside {@code application.port.in} so that other
 * modules can use the return types of {@code programcatalog::api} queries without importing the
 * module's domain layer. Consumers must list {@code programcatalog::api-dto} in their
 * {@code @ApplicationModule(allowedDependencies)} declaration.
 *
 * <p>DTOs exposed via this named interface:
 *
 * <ul>
 *   <li>{@link co.edu.uniquindio.brujula.programcatalog.application.dto.StudyPlanView}
 *   <li>{@link co.edu.uniquindio.brujula.programcatalog.application.dto.ProgramView}
 *   <li>{@link co.edu.uniquindio.brujula.programcatalog.application.dto.JourneyRef}
 *   <li>{@link co.edu.uniquindio.brujula.programcatalog.application.dto.PagedResult}
 * </ul>
 */
@org.springframework.modulith.NamedInterface(name = "api-dto")
package co.edu.uniquindio.brujula.programcatalog.application.dto;

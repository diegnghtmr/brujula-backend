/**
 * Inbound port interfaces for the programcatalog module.
 *
 * <p>Named interface {@code api}: exposed to other modules that need to query programs, study
 * plans, or journeys. Consumers must list {@code programcatalog::api} in their
 * {@code @ApplicationModule(allowedDependencies)} declaration.
 *
 * <p>Interfaces exposed via this named interface:
 *
 * <ul>
 *   <li>{@link co.edu.uniquindio.brujula.programcatalog.application.port.in.GetProgramQuery}
 *   <li>{@link co.edu.uniquindio.brujula.programcatalog.application.port.in.GetStudyPlanQuery}
 *   <li>{@link co.edu.uniquindio.brujula.programcatalog.application.port.in.ListJourneysQuery}
 *   <li>{@link co.edu.uniquindio.brujula.programcatalog.application.port.in.ListProgramPlansQuery}
 *   <li>{@link co.edu.uniquindio.brujula.programcatalog.application.port.in.ListProgramsQuery}
 * </ul>
 */
@org.springframework.modulith.NamedInterface(name = "api")
package co.edu.uniquindio.brujula.programcatalog.application.port.in;

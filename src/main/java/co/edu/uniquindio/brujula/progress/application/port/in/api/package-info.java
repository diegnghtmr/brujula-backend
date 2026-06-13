/**
 * Published API for the progress module — read-only query interfaces for cross-module consumption.
 *
 * <p>This sub-package is published as {@code @NamedInterface("api")} so that other modules (e.g.
 * simulation) can depend on {@code progress::api} without gaining access to progress mutation
 * use-cases or internal REST query interfaces.
 *
 * <p>Only {@link
 * co.edu.uniquindio.brujula.progress.application.port.in.api.GetStudentCourseHistoryQuery} is
 * exposed here. Mutation use-cases (CreateSubscriptionUseCase, CreateCourseAttemptUseCase, etc.)
 * and internal read queries (GetSubscriptionQuery, GetCourseAttemptQuery, etc.) remain in the
 * parent package {@code port.in} which carries NO named interface.
 *
 * <p>Progress's own REST controllers import from {@code port.in} directly (same module — no named
 * interface restriction applies to intra-module access).
 */
@org.springframework.modulith.NamedInterface("api")
package co.edu.uniquindio.brujula.progress.application.port.in.api;

/**
 * Internal inbound ports for the progress module — use-case and query interfaces.
 *
 * <p>This package carries NO {@code @NamedInterface} annotation. Mutation use-cases
 * (CreateSubscriptionUseCase, UpdateSubscriptionUseCase, DeleteSubscriptionUseCase,
 * CreateCourseAttemptUseCase, UpdateCourseAttemptUseCase, DeleteCourseAttemptUseCase) and internal
 * read queries (GetSubscriptionQuery, GetCourseAttemptQuery, etc.) are internal to the progress
 * module and must NOT be consumed by other modules.
 *
 * <p>The cross-module published read query ({@code GetStudentCourseHistoryQuery}) has been moved to
 * the sub-package {@code port.in.api} which carries {@code @NamedInterface("api")}. Progress's own
 * REST controllers may import from this package directly (intra-module access is always allowed by
 * Spring Modulith).
 */
package co.edu.uniquindio.brujula.progress.application.port.in;

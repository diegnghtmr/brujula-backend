/**
 * Internal application DTOs for the progress module — commands, queries, and view records.
 *
 * <p>This package carries NO {@code @NamedInterface} annotation. Mutation command records
 * (CreateSubscriptionCommand, CreateCourseAttemptCommand, UpdateSubscriptionCommand,
 * UpdateCourseAttemptCommand) and internal view records are internal to the progress module.
 *
 * <p>No cross-module read view type is currently published by {@code progress::api-dto}. When WU3
 * introduces a view type referenced by a published api method signature, a sub-package {@code
 * dto/api/} with {@code @NamedInterface("api-dto")} will be created at that time.
 */
package co.edu.uniquindio.brujula.progress.application.dto;

package co.edu.uniquindio.brujula.curriculum.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.dto.UpdateCourseCommand;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.service.UpdateCourseService;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * TDD RED → GREEN: unit tests for {@link UpdateCourseService}.
 *
 * <p>Covers: successful update returns new view, audit recorded; not found throws; stale-version
 * propagates OptimisticLockingFailureException.
 */
class UpdateCourseServiceTest {

  private SaveCoursePort saveCoursePort;
  private AuditRecorder auditRecorder;
  private UpdateCourseService service;

  private final UUID planId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final CourseId courseId = CourseId.generate();

  @BeforeEach
  void setUp() {
    saveCoursePort = mock(SaveCoursePort.class);
    auditRecorder = mock(AuditRecorder.class);
    service = new UpdateCourseService(saveCoursePort, auditRecorder);
  }

  @Test
  void updates_course_fields_and_records_audit() {
    Course existing =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            null,
            "Cálculo I",
            CourseType.REQUIRED,
            4,
            4,
            Collections.emptyList(),
            CourseStatus.ACTIVE,
            1L);
    when(saveCoursePort.findCurrentById(courseId)).thenReturn(Optional.of(existing));

    Course updated =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            null,
            "Cálculo II",
            CourseType.OPTIONAL,
            5,
            5,
            Collections.emptyList(),
            CourseStatus.ACTIVE,
            2L);
    when(saveCoursePort.save(any(Course.class))).thenReturn(updated);

    UpdateCourseCommand command =
        new UpdateCourseCommand(
            actorId, courseId.value(), 1L, "Cálculo II", null, "OPTIONAL", 5, 5, List.of());

    CourseView result = service.updateCourse(command);

    assertThat(result.name()).isEqualTo("Cálculo II");
    assertThat(result.version()).isEqualTo(2L);
    verify(auditRecorder).record(argThat(cmd -> "COURSE_UPDATED".equals(cmd.action())));
  }

  @Test
  void not_found_throws_CourseNotFoundException() {
    when(saveCoursePort.findCurrentById(courseId)).thenReturn(Optional.empty());

    UpdateCourseCommand command =
        new UpdateCourseCommand(
            actorId, courseId.value(), 0L, "Name", null, "REQUIRED", 3, 3, List.of());

    assertThatThrownBy(() -> service.updateCourse(command))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void stale_version_propagates_optimistic_lock_exception() {
    Course existing =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            null,
            "Cálculo I",
            CourseType.REQUIRED,
            4,
            4,
            Collections.emptyList(),
            CourseStatus.ACTIVE,
            3L);
    when(saveCoursePort.findCurrentById(courseId)).thenReturn(Optional.of(existing));
    when(saveCoursePort.save(any(Course.class)))
        .thenThrow(new OptimisticLockingFailureException("Version conflict"));

    UpdateCourseCommand command =
        new UpdateCourseCommand(
            actorId, courseId.value(), 2L, "Cálculo I", null, "REQUIRED", 4, 4, List.of());

    assertThatThrownBy(() -> service.updateCourse(command))
        .isInstanceOf(OptimisticLockingFailureException.class);
  }
}

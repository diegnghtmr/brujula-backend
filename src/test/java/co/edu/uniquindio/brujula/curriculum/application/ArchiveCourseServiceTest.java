package co.edu.uniquindio.brujula.curriculum.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.audit.application.port.in.AuditRecorder;
import co.edu.uniquindio.brujula.curriculum.application.dto.ArchiveCourseCommand;
import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.out.SaveCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.service.ArchiveCourseService;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * TDD RED → GREEN: unit tests for {@link ArchiveCourseService}.
 *
 * <p>Covers: successful archive transitions status, audit recorded; not found throws; stale version
 * propagates OptimisticLockingFailureException.
 */
class ArchiveCourseServiceTest {

  private SaveCoursePort saveCoursePort;
  private AuditRecorder auditRecorder;
  private ArchiveCourseService service;

  private final UUID planId = UUID.randomUUID();
  private final UUID programId = UUID.randomUUID();
  private final UUID actorId = UUID.randomUUID();
  private final CourseId courseId = CourseId.generate();

  @BeforeEach
  void setUp() {
    saveCoursePort = mock(SaveCoursePort.class);
    auditRecorder = mock(AuditRecorder.class);
    service = new ArchiveCourseService(saveCoursePort, auditRecorder);
  }

  @Test
  void archives_course_and_records_audit() {
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

    Course archived =
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
            CourseStatus.ARCHIVED,
            2L);
    when(saveCoursePort.save(any(Course.class))).thenReturn(archived);

    ArchiveCourseCommand command = new ArchiveCourseCommand(actorId, courseId.value(), 1L);
    CourseView result = service.archiveCourse(command);

    assertThat(result.status()).isEqualTo("ARCHIVED");
    verify(auditRecorder).record(argThat(cmd -> "COURSE_ARCHIVED".equals(cmd.action())));
  }

  @Test
  void not_found_throws_CourseNotFoundException() {
    when(saveCoursePort.findCurrentById(courseId)).thenReturn(Optional.empty());

    ArchiveCourseCommand command = new ArchiveCourseCommand(actorId, courseId.value(), 0L);

    assertThatThrownBy(() -> service.archiveCourse(command))
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

    ArchiveCourseCommand command = new ArchiveCourseCommand(actorId, courseId.value(), 2L);

    assertThatThrownBy(() -> service.archiveCourse(command))
        .isInstanceOf(OptimisticLockingFailureException.class);
  }
}

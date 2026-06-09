package co.edu.uniquindio.brujula.curriculum.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.GetCourseQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCoursePort;
import co.edu.uniquindio.brujula.curriculum.application.service.GetCourseService;
import co.edu.uniquindio.brujula.curriculum.domain.Course;
import co.edu.uniquindio.brujula.curriculum.domain.CourseId;
import co.edu.uniquindio.brujula.curriculum.domain.CourseNotFoundException;
import co.edu.uniquindio.brujula.curriculum.domain.CourseStatus;
import co.edu.uniquindio.brujula.curriculum.domain.CourseType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU1-4.3 RED: Verifies GetCourseService returns a CourseView for found courses and throws
 * CourseNotFoundException (404-mapped domain exception) for missing courses.
 */
class GetCourseServiceTest {

  private LoadCoursePort loadCoursePort;
  private GetCourseQuery service;
  private UUID planId;
  private UUID programId;

  @BeforeEach
  void setUp() {
    loadCoursePort = mock(LoadCoursePort.class);
    service = new GetCourseService(loadCoursePort);
    planId = UUID.randomUUID();
    programId = UUID.randomUUID();
  }

  @Test
  void returnsCourseViewForExistingCourse() {
    CourseId courseId = CourseId.generate();
    Course course =
        Course.reconstitute(
            courseId,
            planId,
            programId,
            "ISC-S3-INGSOFTWARE",
            "Ingeniería de Software I",
            CourseType.REQUIRED,
            3,
            4,
            List.of(),
            CourseStatus.ACTIVE,
            0L);

    when(loadCoursePort.loadById(courseId)).thenReturn(Optional.of(course));

    CourseView view = service.getCourse(courseId.value());

    assertThat(view.id()).isEqualTo(courseId.value());
    assertThat(view.name()).isEqualTo("Ingeniería de Software I");
    assertThat(view.officialCode()).isEqualTo("ISC-S3-INGSOFTWARE");
  }

  @Test
  void throwsCourseNotFoundExceptionForMissingCourse() {
    CourseId missingId = CourseId.generate();
    when(loadCoursePort.loadById(missingId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getCourse(missingId.value()))
        .isInstanceOf(CourseNotFoundException.class);
  }

  @Test
  void courseNotFoundExceptionContainsCourseId() {
    CourseId missingId = CourseId.generate();
    when(loadCoursePort.loadById(missingId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getCourse(missingId.value()))
        .isInstanceOf(CourseNotFoundException.class)
        .hasMessageContaining(missingId.value().toString());
  }
}

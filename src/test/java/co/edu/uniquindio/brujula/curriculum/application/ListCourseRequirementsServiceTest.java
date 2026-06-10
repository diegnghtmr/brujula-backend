package co.edu.uniquindio.brujula.curriculum.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.brujula.curriculum.application.dto.CourseRequirementView;
import co.edu.uniquindio.brujula.curriculum.application.port.in.ListCourseRequirementsQuery;
import co.edu.uniquindio.brujula.curriculum.application.port.out.LoadCourseRequirementPort;
import co.edu.uniquindio.brujula.curriculum.application.service.ListCourseRequirementsService;
import co.edu.uniquindio.brujula.curriculum.domain.Confidence;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirement;
import co.edu.uniquindio.brujula.curriculum.domain.CourseRequirementId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * WU1-4.5 RED: Verifies ListCourseRequirementsService returns all requirement fields including
 * confidence and inferred in CourseRequirementView.
 */
class ListCourseRequirementsServiceTest {

  private LoadCourseRequirementPort loadCourseRequirementPort;
  private ListCourseRequirementsQuery service;
  private UUID targetCourseId;
  private UUID requiredCourseId;

  @BeforeEach
  void setUp() {
    loadCourseRequirementPort = mock(LoadCourseRequirementPort.class);
    service = new ListCourseRequirementsService(loadCourseRequirementPort);
    targetCourseId = UUID.randomUUID();
    requiredCourseId = UUID.randomUUID();
  }

  @Test
  void returnsAllRequirementFieldsIncludingConfidenceAndInferred() {
    CourseRequirement req =
        CourseRequirement.coursePassed(
            CourseRequirementId.generate(),
            targetCourseId,
            requiredCourseId,
            false,
            Confidence.HIGH,
            false);

    when(loadCourseRequirementPort.loadByTargetCourseId(targetCourseId)).thenReturn(List.of(req));

    List<CourseRequirementView> views = service.listCourseRequirements(targetCourseId);

    assertThat(views).hasSize(1);
    CourseRequirementView view = views.get(0);
    assertThat(view.targetCourseId()).isEqualTo(targetCourseId);
    assertThat(view.requiredCourseId()).isEqualTo(requiredCourseId);
    assertThat(view.type()).isEqualTo("COURSE_PASSED");
    assertThat(view.confidence()).isEqualTo("HIGH");
    assertThat(view.inferred()).isFalse();
  }

  @Test
  void returnsInferredTrueAndMediumConfidence() {
    CourseRequirement req =
        CourseRequirement.coursePassed(
            CourseRequirementId.generate(),
            targetCourseId,
            requiredCourseId,
            false,
            Confidence.MEDIUM,
            true);

    when(loadCourseRequirementPort.loadByTargetCourseId(targetCourseId)).thenReturn(List.of(req));

    List<CourseRequirementView> views = service.listCourseRequirements(targetCourseId);

    assertThat(views.get(0).confidence()).isEqualTo("MEDIUM");
    assertThat(views.get(0).inferred()).isTrue();
  }

  @Test
  void returnsNullConfidenceWhenNotSet() {
    CourseRequirement req =
        CourseRequirement.coursePassed(
            CourseRequirementId.generate(), targetCourseId, requiredCourseId, false, null, null);

    when(loadCourseRequirementPort.loadByTargetCourseId(targetCourseId)).thenReturn(List.of(req));

    List<CourseRequirementView> views = service.listCourseRequirements(targetCourseId);

    assertThat(views.get(0).confidence()).isNull();
    assertThat(views.get(0).inferred()).isNull();
  }

  @Test
  void returnsEmptyListWhenNoRequirements() {
    when(loadCourseRequirementPort.loadByTargetCourseId(targetCourseId)).thenReturn(List.of());

    List<CourseRequirementView> views = service.listCourseRequirements(targetCourseId);

    assertThat(views).isEmpty();
  }

  @Test
  void minimumCreditsRequirementHasNullRequiredCourseId() {
    CourseRequirement req =
        CourseRequirement.minimumCredits(CourseRequirementId.generate(), targetCourseId, 60);

    when(loadCourseRequirementPort.loadByTargetCourseId(targetCourseId)).thenReturn(List.of(req));

    List<CourseRequirementView> views = service.listCourseRequirements(targetCourseId);

    assertThat(views.get(0).type()).isEqualTo("MINIMUM_CREDITS");
    assertThat(views.get(0).requiredCourseId()).isNull();
    assertThat(views.get(0).requiredCredits()).isEqualTo(60);
  }
}

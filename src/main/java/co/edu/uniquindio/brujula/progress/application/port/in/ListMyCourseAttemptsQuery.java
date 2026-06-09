package co.edu.uniquindio.brujula.progress.application.port.in;

import co.edu.uniquindio.brujula.progress.application.dto.CourseAttemptPageView;
import co.edu.uniquindio.brujula.progress.application.dto.ListCourseAttemptsQuery;

/** Input port: list course attempts for the authenticated student with optional filters (PRG-2). */
public interface ListMyCourseAttemptsQuery {

  /**
   * List attempts matching the given query parameters.
   *
   * @param query list parameters (actorId, subscriptionId filter, courseId filter, pagination)
   * @return paged result of matching attempts
   */
  CourseAttemptPageView listMyCourseAttempts(ListCourseAttemptsQuery query);
}

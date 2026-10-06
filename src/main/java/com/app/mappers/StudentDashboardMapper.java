package com.app.mappers;

import com.app.api.dto.Course;
import com.app.api.dto.StudentSummary;
import com.app.models.StudentCourseModel;

/** Maps remote course and attendance DTOs into dashboard presentation data. */
public final class StudentDashboardMapper {

  private StudentDashboardMapper() {}

  public static StudentCourseModel toModel(Course course, StudentSummary summary) {
    return new StudentCourseModel(
        course.code(),
        course.name(),
        course.section(),
        course.teacher(),
        course.schedule(),
        summary.taughtHours(),
        summary.attendedHours(),
        summary.excusedHours(),
        summary.exemptHours(),
        summary.percentage(),
        summary.remainingHours(),
        summary.marginHours(),
        summary.risk());
  }
}

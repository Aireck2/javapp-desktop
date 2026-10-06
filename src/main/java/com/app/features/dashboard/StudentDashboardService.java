package com.app.features.dashboard;

import com.app.api.ApiClient;
import com.app.mappers.StudentDashboardMapper;
import com.app.models.StudentCourseModel;
import java.util.List;

/** Coordinates dashboard data retrieval and mapping through the API contract. */
public final class StudentDashboardService {

  private final ApiClient api;
  private final String studentId;

  public StudentDashboardService(ApiClient api, String studentId) {
    this.api = api;
    this.studentId = studentId;
  }

  public List<StudentCourseModel> load() {
    return api.coursesForCurrentUser().stream()
        .map(
            course ->
                StudentDashboardMapper.toModel(course, api.studentSummary(course.id(), studentId)))
        .toList();
  }
}

package com.app.features.attendance;

import com.app.api.ApiClient;
import com.app.api.ApiException;
import com.app.api.dto.ClassSession;
import com.app.api.dto.Course;
import com.app.api.dto.Student;
import com.app.api.dto.StudentSummary;
import com.app.models.StudentAttendanceModel;
import java.util.List;
import java.util.Map;

/** API-facing attendance use cases, returning presentation-ready student models. */
public final class AttendanceService {

  public record LoadedAttendance(
      Course course, List<StudentAttendanceModel> students, String observation) {
    public LoadedAttendance {
      students = List.copyOf(students);
    }
  }

  private final ApiClient api;

  public AttendanceService(ApiClient api) {
    this.api = api;
  }

  public LoadedAttendance load(
      ClassSession session, Map<String, boolean[]> draftMarks, String draftObservation) {
    Course course = findCourse(session.courseId());
    List<StudentAttendanceModel> students =
        loadRoster(course, session, api.attendanceForSession(session.id()), draftMarks);
    String observation =
        draftObservation == null ? api.attendanceObservation(session.id()) : draftObservation;
    return new LoadedAttendance(course, students, observation == null ? "" : observation);
  }

  public List<StudentAttendanceModel> save(
      ClassSession session, Course course, Map<String, boolean[]> marks, String observation) {
    api.saveAttendance(session.id(), marks, observation);
    try {
      return loadRoster(course, session, api.attendanceForSession(session.id()), Map.of());
    } catch (ApiException exception) {
      throw exception;
    }
  }

  private List<StudentAttendanceModel> loadRoster(
      Course course,
      ClassSession session,
      Map<String, boolean[]> savedMarks,
      Map<String, boolean[]> draftMarks) {
    List<Student> roster = api.studentsByCourse(course.id());
    Map<String, StudentSummary> summaries = new java.util.HashMap<>();
    for (Student student : roster) {
      summaries.put(student.id(), api.studentSummary(course.id(), student.id()));
    }
    return AttendanceRosterMapper.toModels(
        course, session.blockHours(), roster, summaries, savedMarks, draftMarks);
  }

  private Course findCourse(String courseId) {
    try {
      return api.coursesForCurrentUser().stream()
          .filter(item -> item.id().equals(courseId))
          .findFirst()
          .orElseGet(() -> new Course(courseId, courseId, courseId, 0));
    } catch (ApiException exception) {
      return new Course(courseId, courseId, courseId, 0);
    }
  }
}

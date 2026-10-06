package com.app.features.courses;

import com.app.api.ApiClient;
import com.app.api.ApiException;
import com.app.api.dto.ClassSession;
import com.app.api.dto.ClassSession.SessionStatus;
import com.app.api.dto.Course;
import com.app.mappers.CourseModelMapper;
import com.app.models.CourseModel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Loads and maps the teacher course timeline without depending on JavaFX nodes. */
public final class CoursesService {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("EEEE dd/MM", Locale.forLanguageTag("es"));

  public record SessionCourse(ClassSession session, CourseModel course) {}

  public record CourseDay(LocalDate date, boolean today, List<SessionCourse> sessions) {
    public CourseDay {
      sessions = List.copyOf(sessions);
    }

    public String title() {
      String dateText = date.format(DATE_FORMATTER);
      dateText =
          dateText.substring(0, 1).toUpperCase(Locale.forLanguageTag("es")) + dateText.substring(1);
      return (today ? "📌 HOY · " : "📅 ") + dateText;
    }
  }

  public record Timeline(List<CourseDay> days, boolean empty) {
    public Timeline {
      days = List.copyOf(days);
    }
  }

  private final ApiClient api;

  public CoursesService(ApiClient api) {
    this.api = api;
  }

  public Timeline loadTimeline() {
    LocalDate today = LocalDate.now();
    List<ClassSession> windowSessions = api.sessionsWindow(today, today.plusDays(5));
    if (windowSessions.isEmpty()) {
      return new Timeline(List.of(), true);
    }

    List<Course> courses = api.coursesForCurrentUser();
    Map<String, Course> coursesById =
        courses.stream()
            .collect(Collectors.toMap(Course::id, course -> course, (first, ignored) -> first));
    Map<String, Integer> enrolled = new HashMap<>();
    Map<String, Integer> completedHours = new HashMap<>();
    for (Course course : courses) {
      try {
        enrolled.put(course.id(), api.studentsByCourse(course.id()).size());
        completedHours.put(
            course.id(),
            api.validSessions(course.id()).stream()
                .filter(session -> session.status() == SessionStatus.DICTADA)
                .mapToInt(ClassSession::blockHours)
                .sum());
      } catch (ApiException ignored) {
        enrolled.put(course.id(), 0);
        completedHours.put(course.id(), 0);
      }
    }

    Map<LocalDate, List<ClassSession>> sessionsByDate =
        windowSessions.stream()
            .collect(Collectors.groupingBy(ClassSession::date, TreeMap::new, Collectors.toList()));
    List<CourseDay> days = new java.util.ArrayList<>();
    for (int offset = 0; offset <= 5; offset++) {
      LocalDate date = today.plusDays(offset);
      List<SessionCourse> sessions =
          sessionsByDate.getOrDefault(date, List.of()).stream()
              .map(
                  session -> {
                    Course course = coursesById.get(session.courseId());
                    CourseModel model =
                        CourseModelMapper.toModel(
                            course,
                            session,
                            enrolled.getOrDefault(session.courseId(), 0),
                            completedHours.getOrDefault(session.courseId(), 0));
                    return new SessionCourse(session, model);
                  })
              .toList();
      days.add(new CourseDay(date, offset == 0, sessions));
    }
    return new Timeline(days, false);
  }
}

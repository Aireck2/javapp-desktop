package com.app.mappers;

import com.app.api.dto.ClassSession;
import com.app.api.dto.Course;
import com.app.models.CourseModel;
import java.time.LocalDate;

/** Mapper para transformar entidades de API en modelos de presentación {@link CourseModel}. */
public final class CourseModelMapper {

  private CourseModelMapper() {
    // Utility class
  }

  /**
   * Mapea un {@link Course} y una sesión asociada (opcional) a {@link CourseModel}.
   *
   * @param course Curso base proveniente de la API
   * @param session Sesión de clase asociada (puede ser null)
   * @param enrolledStudents Cantidad de alumnos matriculados
   * @param completedHours Horas completadas/dictadas acumuladas
   * @return Instancia inmutable de {@link CourseModel}
   */
  public static CourseModel toModel(
      Course course, ClassSession session, int enrolledStudents, int completedHours) {

    String id = course != null ? course.id() : (session != null ? session.courseId() : "");
    String code = course != null ? course.code() : "CURSO";
    String section = course != null ? course.section() : "SEC-01";
    String name =
        course != null
            ? course.name()
            : (session != null ? session.courseId() : "Curso sin nombre");
    String schedule = course != null ? course.schedule() : "Horario pendiente";
    int totalHours = course != null && course.totalHours() > 0 ? course.totalHours() : 40;

    String badgeType = resolveBadgeType(course, session);
    String classroom = resolveClassroom();

    return new CourseModel(
        id,
        code,
        section,
        name,
        schedule,
        classroom,
        badgeType,
        enrolledStudents,
        completedHours,
        totalHours);
  }

  private static String resolveBadgeType(Course course, ClassSession session) {
    if (session != null && LocalDate.now().isEqual(session.date())) {
      return "Clase Hoy";
    }
    if (course != null && course.name() != null) {
      String upper = course.name().toUpperCase();
      if (upper.contains("LAB") || upper.contains("LABORATORIO")) {
        return "Laboratorio";
      }
    }
    return "Teoría";
  }

  private static String resolveClassroom() {
    return "Aula no especificada";
  }
}

package com.app.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.api.dto.ClassSession;
import com.app.api.dto.Course;
import com.app.models.CourseModel;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CourseModelMapperTest {

  @Test
  @DisplayName("Mapea curso con sesión de hoy asignando badge 'Clase Hoy'")
  void mapsCourseWithSessionToday() {
    Course course =
        new Course(
            "c-01", "INF-301", "Desarrollo Móvil", 60, "SEC-01", "Prof. Pérez", "Lun 10:00-12:00");
    ClassSession session =
        new ClassSession("s-01", "c-01", LocalDate.now(), 2, ClassSession.SessionStatus.PROGRAMADA);

    CourseModel model = CourseModelMapper.toModel(course, session, 30, 24);

    assertThat(model.id()).isEqualTo("c-01");
    assertThat(model.code()).isEqualTo("INF-301");
    assertThat(model.section()).isEqualTo("SEC-01");
    assertThat(model.name()).isEqualTo("Desarrollo Móvil");
    assertThat(model.badgeType()).isEqualTo("Clase Hoy");
    assertThat(model.enrolledStudents()).isEqualTo(30);
    assertThat(model.completedHours()).isEqualTo(24);
    assertThat(model.totalHours()).isEqualTo(60);
    assertThat(model.getProgressPercentage()).isEqualTo(40);
  }

  @Test
  @DisplayName("Mapea curso de laboratorio sin sesión hoy asignando badge 'Laboratorio'")
  void mapsLabCourseWithoutTodaySession() {
    Course course =
        new Course(
            "c-02",
            "INF-302",
            "Laboratorio de Software",
            40,
            "SEC-02",
            "Prof. Gómez",
            "Vie 14:00-18:00");
    ClassSession session =
        new ClassSession(
            "s-02", "c-02", LocalDate.now().plusDays(2), 4, ClassSession.SessionStatus.PROGRAMADA);

    CourseModel model = CourseModelMapper.toModel(course, session, 20, 10);

    assertThat(model.badgeType()).isEqualTo("Laboratorio");
    assertThat(model.classroom()).isEqualTo("Aula no especificada");
  }

  @Test
  @DisplayName("Mapea con entidades nulas sin lanzar excepciones")
  void handlesNullInputsGracefully() {
    CourseModel model = CourseModelMapper.toModel(null, null, 0, 0);

    assertThat(model).isNotNull();
    assertThat(model.name()).isEqualTo("Curso sin nombre");
    assertThat(model.badgeType()).isEqualTo("Teoría");
  }
}

package com.app.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CourseModelTest {

  @Test
  @DisplayName("Calcula porcentaje de progreso correctamente")
  void calculatesProgressPercentage() {
    CourseModel course =
        new CourseModel(
            "c1",
            "MAT-101",
            "SEC-A",
            "Matemática",
            "Lun-Mie 08:00",
            "Aula 101",
            "Clase Hoy",
            25,
            20,
            40);

    assertThat(course.getProgressPercentage()).isEqualTo(50);
  }

  @Test
  @DisplayName("Retorna 0 si totalHours es 0 o negativo")
  void zeroTotalHoursReturnsZero() {
    CourseModel course =
        new CourseModel(
            "c1",
            "MAT-101",
            "SEC-A",
            "Matemática",
            "Lun-Mie 08:00",
            "Aula 101",
            "Clase Hoy",
            25,
            10,
            0);

    assertThat(course.getProgressPercentage()).isEqualTo(0);
  }

  @Test
  @DisplayName("Redondea y limita el porcentaje entre 0 y 100")
  void clampsPercentageBetween0And100() {
    CourseModel course =
        new CourseModel(
            "c1",
            "MAT-101",
            "SEC-A",
            "Matemática",
            "Lun-Mie 08:00",
            "Aula 101",
            "Clase Hoy",
            25,
            50,
            40);

    assertThat(course.getProgressPercentage()).isEqualTo(100);
  }

  @Test
  @DisplayName("Maneja valores nulos con valores por defecto seguros")
  void handlesNullValuesWithDefaults() {
    CourseModel course = new CourseModel(null, null, null, null, null, null, null, -5, -2, -10);

    assertThat(course.id()).isEmpty();
    assertThat(course.code()).isEmpty();
    assertThat(course.section()).isEmpty();
    assertThat(course.name()).isEmpty();
    assertThat(course.classroom()).isEqualTo("Aula General");
    assertThat(course.badgeType()).isEqualTo("Regular");
    assertThat(course.enrolledStudents()).isEqualTo(0);
    assertThat(course.completedHours()).isEqualTo(0);
    assertThat(course.totalHours()).isEqualTo(0);
    assertThat(course.getProgressPercentage()).isEqualTo(0);
  }
}

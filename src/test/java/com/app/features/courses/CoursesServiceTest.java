package com.app.features.courses;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.api.MockApiClient;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CoursesServiceTest {

  @Test
  void mapsMockTimelineIntoSixDaysAndPreservesTodaySession() {
    CoursesService.Timeline timeline = new CoursesService(new MockApiClient()).loadTimeline();

    assertThat(timeline.empty()).isFalse();
    assertThat(timeline.days()).hasSize(6);
    assertThat(timeline.days().getFirst().date()).isEqualTo(LocalDate.now());
    assertThat(timeline.days().getFirst().today()).isTrue();
    assertThat(timeline.days().getFirst().sessions())
        .anySatisfy(
            entry -> {
              assertThat(entry.session().id()).isEqualTo("ses-hoy-001");
              assertThat(entry.course().badgeType()).isEqualTo("Clase Hoy");
              assertThat(entry.course().enrolledStudents()).isEqualTo(3);
            });
  }
}

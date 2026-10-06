package com.app.features.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.api.MockApiClient;
import org.junit.jupiter.api.Test;

class StudentDashboardServiceTest {

  @Test
  void mapsMockCourseSummariesToPresentationModels() {
    var courses = new StudentDashboardService(new MockApiClient(), "alu-01").load();

    assertThat(courses).hasSize(2);
    assertThat(courses.getFirst().code()).isEqualTo("MAT-101");
    assertThat(courses.getFirst().taughtHours()).isEqualTo(2);
    assertThat(courses.getFirst().risk()).isNotNull();
  }
}

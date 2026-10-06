package com.app.features.courses;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.api.MockApiClient;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CoursesFxmlViewTest {

  @BeforeAll
  static void initJavaFx() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException ignored) {
      // The toolkit was started by another test.
    }
  }

  @Test
  void loadsAndAppliesCssToCourseTimeline() {
    CoursesView view = new CoursesView(new MockApiClient(), session -> {}, session -> {});
    new Scene(view);
    view.applyCss();
    view.layout();

    assertThat(view.lookup(".screen-title")).isNotNull();
    assertThat(view.lookup("#stateContainer")).isNotNull();
    assertThat(view.getStylesheets()).anyMatch(path -> path.endsWith("/com/app/css/theme.css"));
  }
}

package com.app.features.attendance;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.api.MockApiClient;
import com.app.api.dto.ClassSession;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AttendanceDetailFxmlViewTest {

  @BeforeAll
  static void initJavaFx() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException ignored) {
      // The toolkit was started by another test.
    }
  }

  @Test
  void loadsAttendanceScreenFromFxmlWithSessionContext() {
    MockApiClient api = new MockApiClient();
    ClassSession session =
        api
            .sessionsWindow(java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(5))
            .stream()
            .filter(item -> item.id().equals("ses-hoy-001"))
            .findFirst()
            .orElseThrow();

    AttendanceDetailView view = new AttendanceDetailView(api, session, () -> {});
    new Scene(view);
    view.applyCss();
    view.layout();

    assertThat(view.lookup(".attendance-session-header")).isNotNull();
    assertThat(view.lookup(".attendance-summary")).isNotNull();
    assertThat(view.lookup("#studentList")).isNotNull();
    assertThat(view.getStylesheets()).anyMatch(path -> path.endsWith("/com/app/css/theme.css"));
  }
}

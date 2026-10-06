package com.app.features.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.auth.UserSession;
import com.app.shared.fxml.FxmlViewLoader;
import java.time.Instant;
import java.util.Set;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProfileFxmlViewTest {

  @BeforeAll
  static void initJavaFx() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException ignored) {
      // The toolkit was started by another test.
    }
  }

  @Test
  void loadsProfileFxmlWithInjectedSessionAndAppStylesheet() {
    UserSession session = UserSession.getInstance();
    session.login(
        "test-access-token",
        "test-user",
        "Test User",
        Set.of("DOCENTE"),
        Instant.now().plusSeconds(600));

    Parent view =
        FxmlViewLoader.load(
            "/com/app/features/profile/profile.fxml",
            type -> {
              assertThat(type).isEqualTo(ProfileController.class);
              return new ProfileController(session);
            });
    new Scene(view);
    view.applyCss();
    view.layout();

    assertThat(label(view, "displayName").getText()).isEqualTo("Test User");
    assertThat(label(view, "username").getText()).isEqualTo("test-user");
    assertThat(label(view, "roles").getText()).isEqualTo("DOCENTE");
    assertThat(view.getStylesheets()).anyMatch(path -> path.endsWith("/com/app/css/theme.css"));

    session.logout();
  }

  private static Label label(Parent view, String id) {
    return (Label) view.lookup("#" + id);
  }
}

package com.app.features.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.api.MockApiClient;
import com.app.auth.TokenStore;
import com.app.shared.fxml.FxmlViewLoader;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AuthFxmlViewsTest {

  @BeforeAll
  static void initJavaFx() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException ignored) {
      // The toolkit was started by another test.
    }
  }

  @Test
  void loadsLoginFormFromFxml() {
    LoginScreen view = new LoginScreen(new MockApiClient(), new TokenStore(), tokens -> {});
    new Scene(view);
    view.applyCss();
    view.layout();

    assertThat(view.lookup("#usernameField")).isNotNull();
    assertThat(view.lookup("#passwordField")).isNotNull();
    assertThat(view.lookup("#loginButton")).isNotNull();
    assertThat(view.getStylesheets()).anyMatch(path -> path.endsWith("/com/app/css/theme.css"));
  }

  @Test
  void loadsSplashStructureWithoutStartingAnimationDuringResourceCheck() {
    VBox view = new VBox();
    FxmlViewLoader.loadInto(
        view,
        "/com/app/features/auth/splash.fxml",
        new SplashController(
            new SplashViewModel(new AuthenticationService(null, null)), null, null, false));
    new Scene(view);
    view.applyCss();
    view.layout();

    assertThat(view.lookup("#progressBar")).isNotNull();
    assertThat(view.lookup("#statusLabel")).isNotNull();
    assertThat(view.lookup("#percentLabel")).isNotNull();
  }
}

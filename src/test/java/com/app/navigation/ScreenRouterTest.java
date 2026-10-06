package com.app.navigation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScreenRouterTest {

  private StackPane container;
  private ScreenRouter router;

  @BeforeAll
  static void initJavaFx() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException ignored) {
      // JavaFX toolkit ya inicializado
    }
  }

  @BeforeEach
  void setUp() {
    container = new StackPane();
    router = new ScreenRouter(container);
  }

  @Test
  void registerAndNavigateToRoute() {
    router.registerView("home", data -> new Label("Home View"));

    router.navigateTo("home");

    assertThat(router.getCurrentRoute()).isEqualTo("home");
    assertThat(router.canGoBack()).isFalse();
  }

  @Test
  void navigatePassingData() {
    AtomicReference<Object> receivedData = new AtomicReference<>();
    router.registerView(
        "profile",
        data -> {
          receivedData.set(data);
          return new Label("Profile: " + data);
        });

    router.navigateTo("profile", "user-123");

    assertThat(router.getCurrentRoute()).isEqualTo("profile");
    assertThat(router.getCurrentData()).isEqualTo("user-123");
    assertThat(receivedData.get()).isEqualTo("user-123");
  }

  @Test
  void navigationHistoryAndGoBack() {
    router.registerView("view1", data -> new Label("View 1"));
    router.registerView("view2", data -> new Label("View 2"));
    router.registerView("view3", data -> new Label("View 3"));

    router.navigateTo("view1");
    router.navigateTo("view2");
    router.navigateTo("view3");

    assertThat(router.getCurrentRoute()).isEqualTo("view3");
    assertThat(router.canGoBack()).isTrue();

    boolean back1 = router.goBack();
    assertThat(back1).isTrue();
    assertThat(router.getCurrentRoute()).isEqualTo("view2");

    boolean back2 = router.goBack();
    assertThat(back2).isTrue();
    assertThat(router.getCurrentRoute()).isEqualTo("view1");

    boolean back3 = router.goBack();
    assertThat(back3).isFalse();
    assertThat(router.getCurrentRoute()).isEqualTo("view1");
  }

  @Test
  void throwsExceptionWhenNavigatingToUnregisteredRoute() {
    assertThatThrownBy(() -> router.navigateTo("non-existent"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Ruta no registrada");
  }

  @Test
  void clearHistoryEmptiesStack() {
    router.registerView("v1", d -> new Label("1"));
    router.registerView("v2", d -> new Label("2"));

    router.navigateTo("v1");
    router.navigateTo("v2");
    assertThat(router.canGoBack()).isTrue();

    router.clearHistory();
    assertThat(router.canGoBack()).isFalse();
  }
}

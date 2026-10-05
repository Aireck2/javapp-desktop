package com.app;

import java.time.Duration;

import com.app.api.ApiClient;
import com.app.api.MockApiClient;
import com.app.auth.SplashView;
import com.app.auth.TokenStore;
import com.app.config.AppConfig;
import com.app.layout.MainShell;
import com.app.navigation.ScreenRouter;
import com.app.session.UserSession;
import com.app.views.LoginView;

import atlantafx.base.theme.PrimerLight;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {

  private static final ApiClient API = new MockApiClient();
  private static final TokenStore TOKENS = new TokenStore();

  private ScreenRouter router;

  @Override
  public void start(Stage stage) {
    // Inicializar tema moderno AtlantaFX
    Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
    stage.setTitle("Javapp Desktop · Portal Académico");

    // Contenedor principal de pantallas
    StackPane rootContent = new StackPane();
    rootContent.setPadding(new Insets(20));

    router = new ScreenRouter(rootContent);

    // Registro de rutas globales
    router.registerView("splash", data -> new SplashView(
        API,
        TOKENS,
        tokens -> router.navigateTo("main"),
        () -> router.navigateTo("login")));

    router.registerView("login", data -> new LoginView(
        API,
        TOKENS,
        tokens -> router.navigateTo("main")));

    router.registerView("main", data -> new MainShell(
        API,
        () -> {
          TOKENS.clear();
          router.navigateTo("login");
        }));

    // Envoltorio Mobile-First

    Scene scene = new Scene(rootContent, 1280, 720);
    stage.setScene(scene);
    stage.setMinWidth(440);
    stage.setMinHeight(800);

    // Iniciar en la pantalla Splash para verificar persistencia de sesión
    router.navigateTo("splash");

    // Control de inactividad
    watchInactivity(stage);

    stage.show();
  }

  /**
   * Vigila la inactividad del usuario (15 min por defecto) y cierra la sesión
   * automáticamente.
   */
  private void watchInactivity(Stage stage) {
    Timeline timer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(60), e -> {
      UserSession session = UserSession.getInstance();
      if (session.isLoggedIn() && session.isInactive(Duration.ofMinutes(AppConfig.inactivityMinutes()))) {
        session.logout();
        TOKENS.clear();
        if (router != null) {
          router.navigateTo("login");
        }
      }
    }));
    timer.setCycleCount(Timeline.INDEFINITE);
    timer.play();

    stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
      if (newScene != null) {
        newScene.addEventFilter(javafx.scene.input.InputEvent.ANY, ev -> UserSession.getInstance().touch());
      }
    });
  }

  public static void main(String[] args) {
    launch(args);
  }
}

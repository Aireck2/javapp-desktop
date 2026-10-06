package com.app.features.auth;

import com.app.api.dto.SessionTokens;
import com.app.config.AppConfig;
import java.io.InputStream;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

/** Presents splash progress while refresh-token validation runs in the background. */
public final class SplashController {

  @FXML private ImageView logo;
  @FXML private ProgressBar progressBar;
  @FXML private Label percentLabel;
  @FXML private Label statusLabel;

  private final SplashViewModel viewModel;
  private final Consumer<SessionTokens> onSuccess;
  private final Runnable onLoginRequired;
  private final boolean startLifecycle;
  private Timeline timeline;
  private double progress;
  private boolean sessionChecked;
  private boolean animationCompleted;
  private boolean navigated;
  private Runnable pendingNavigation;

  public SplashController(
      SplashViewModel viewModel, Consumer<SessionTokens> onSuccess, Runnable onLoginRequired) {
    this(viewModel, onSuccess, onLoginRequired, true);
  }

  SplashController(
      SplashViewModel viewModel,
      Consumer<SessionTokens> onSuccess,
      Runnable onLoginRequired,
      boolean startLifecycle) {
    this.viewModel = viewModel;
    this.onSuccess = onSuccess;
    this.onLoginRequired = onLoginRequired;
    this.startLifecycle = startLifecycle;
  }

  @FXML
  private void initialize() {
    loadLogo();
    if (startLifecycle) {
      startProgress();
      validateSession();
    }
  }

  private void loadLogo() {
    String[] paths = {"/images/logo.png", "/logo_splash.png", "/logo.png"};
    for (String path : paths) {
      try (InputStream stream = getClass().getResourceAsStream(path)) {
        if (stream != null) {
          logo.setImage(new Image(stream));
          return;
        }
      } catch (Exception ignored) {
        // Continue to the next supported legacy logo location.
      }
    }
  }

  private void startProgress() {
    long minDuration = AppConfig.splashMinMillis();
    int steps = 100;
    long stepDuration = Math.max(10, minDuration / steps);
    timeline =
        new Timeline(new KeyFrame(Duration.millis(stepDuration), event -> advanceProgress(steps)));
    timeline.setCycleCount(steps);
    timeline.play();
  }

  private void advanceProgress(int steps) {
    progress = Math.min(1.0, progress + (1.0 / steps));
    progressBar.setProgress(progress);
    int percent = (int) Math.round(progress * 100);
    percentLabel.setText(percent + "%");
    if (percent == 25) {
      statusLabel.setText("Conectando con la infraestructura...");
    } else if (percent == 55) {
      statusLabel.setText("Comprobando sesión institucional...");
    } else if (percent == 85) {
      statusLabel.setText("Sincronización lista...");
    }
    if (percent == 100) {
      animationCompleted = true;
      completeIfReady();
    }
  }

  private void validateSession() {
    viewModel.restoreSession(
        tokens ->
            Platform.runLater(
                () -> {
                  pendingNavigation =
                      tokens
                          .map(
                              value ->
                                  onSuccess == null
                                      ? onLoginRequired
                                      : (Runnable) () -> onSuccess.accept(value))
                          .orElse(onLoginRequired);
                  sessionChecked = true;
                  completeIfReady();
                }));
  }

  private void completeIfReady() {
    if (animationCompleted && sessionChecked && !navigated) {
      navigated = true;
      if (timeline != null) {
        timeline.stop();
      }
      if (pendingNavigation != null) {
        pendingNavigation.run();
      }
    }
  }
}

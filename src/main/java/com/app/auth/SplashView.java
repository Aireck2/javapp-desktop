package com.app.auth;

import com.app.api.ApiClient;
import com.app.api.ApiException;
import com.app.api.dto.SessionTokens;
import com.app.config.AppConfig;
import com.app.navigation.ScreenRouter;
import com.app.session.UserSession;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * Vista Splash / Bienvenida moderna para el Portal Académico (mvp.md §2.1,
 * US-01).
 * Presenta una interfaz mobile-first / desktop-centered acotada a 350px con
 * duración
 * mínima garantizada de 5 segundos (5000 ms) mientras comprueba la sesión en
 * segundo plano.
 */
public class SplashView extends VBox {

  private final ProgressBar progressBar;
  private final Label percentLabel;
  private final Label statusLabel;
  private double progress = 0.0;

  private final AtomicBoolean sessionChecked = new AtomicBoolean(false);
  private final AtomicBoolean animationCompleted = new AtomicBoolean(false);
  private final AtomicReference<Runnable> pendingNavigation = new AtomicReference<>(null);

  public SplashView(ScreenRouter router) {
    this(null, null,
        tokens -> {
          if (router != null)
            router.navigateTo("main");
        },
        () -> {
          if (router != null)
            router.navigateTo("login");
        });
  }

  public SplashView(
      ApiClient api,
      TokenStore tokenStore,
      Consumer<SessionTokens> onSuccess,
      Runnable onLoginRequired) {
    super();
    this.progressBar = new ProgressBar(0);
    this.percentLabel = new Label("0%");
    this.statusLabel = new Label("Verificando credenciales institucionales...");

    initUI();
    startSequence(api, tokenStore, onSuccess, onLoginRequired);
  }

  private void initUI() {
    setAlignment(Pos.CENTER);
    setPadding(new Insets(24, 16, 24, 16));
    setStyle("-fx-background-color: #F8FAFC;");

    // Contenedor principal (350px de ancho)
    VBox mainContainer = new VBox(22);
    mainContainer.setAlignment(Pos.CENTER);
    mainContainer.setMaxWidth(350);
    mainContainer.setMinWidth(300);

    // --- 1. BADGE "PLATAFORMA OFICIAL" ---
    HBox topBadge = new HBox(6);
    topBadge.setAlignment(Pos.CENTER);
    topBadge.setPadding(new Insets(4, 14, 4, 14));
    topBadge.setStyle("-fx-background-color: #EEF2FF; -fx-background-radius: 20px;");

    Circle blueDot = new Circle(3, Color.web("#2563EB"));
    Label badgeLabel = new Label("PLATAFORMA OFICIAL");
    badgeLabel
        .setStyle("-fx-text-fill: #1E40AF; -fx-font-size: 11px; -fx-font-weight: bold; -fx-letter-spacing: 0.5px;");
    topBadge.getChildren().addAll(blueDot, badgeLabel);

    // --- 2. LOGO EN CARD CON CHECK DE VERIFICACIÓN ---
    StackPane logoCard = new StackPane();
    logoCard.setPrefSize(110, 110);
    logoCard.setMaxSize(110, 110);
    logoCard.setStyle(
        "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 24px; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 16, 0, 0, 6);");

    ImageView logoView = new ImageView();
    loadImage(logoView);
    logoView.setFitWidth(76);
    logoView.setFitHeight(76);
    logoView.setPreserveRatio(true);

    // Insignia badge azul en la esquina inferior derecha
    HBox checkBadge = new HBox();
    checkBadge.setAlignment(Pos.CENTER);
    checkBadge.setPrefSize(22, 22);
    checkBadge.setMaxSize(22, 22);
    checkBadge.setStyle("-fx-background-color: #2563EB; -fx-background-radius: 50%;");

    Label checkIcon = new Label("✓");
    checkIcon.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
    checkBadge.getChildren().add(checkIcon);

    StackPane.setAlignment(checkBadge, Pos.BOTTOM_RIGHT);
    StackPane.setMargin(checkBadge, new Insets(0, 4, 4, 0));
    logoCard.getChildren().addAll(logoView, checkBadge);

    // --- 3. TÍTULOS ---
    Label titleLabel = new Label("Portal Académico");
    titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #032B7A;");

    Label subtitleLabel = new Label("Control de Asistencia & Gestión\nUniversitaria");
    subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-text-alignment: center;");
    subtitleLabel.setWrapText(true);

    VBox headerBox = new VBox(6, topBadge, logoCard, titleLabel, subtitleLabel);
    headerBox.setAlignment(Pos.CENTER);

    // --- 4. BARRA DE PROGRESO Y ESTADO ---
    progressBar.setMaxWidth(Double.MAX_VALUE);
    progressBar.setPrefHeight(6);
    progressBar.setStyle(
        "-fx-accent: #2563EB; " +
            "-fx-control-inner-background: #E2E8F0; " +
            "-fx-background-radius: 10px;");

    HBox statusBox = new HBox(6);
    statusBox.setAlignment(Pos.CENTER);
    Circle statusDot = new Circle(3, Color.web("#2563EB"));
    statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
    statusBox.getChildren().addAll(statusDot, statusLabel);

    percentLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #64748B;");

    VBox progressBox = new VBox(10, progressBar, statusBox, percentLabel);
    progressBox.setAlignment(Pos.CENTER);
    progressBox.setPadding(new Insets(10, 0, 0, 0));

    // --- 5. FOOTER SEGURO SSL ---
    HBox sslBadge = new HBox(6);
    sslBadge.setAlignment(Pos.CENTER);
    sslBadge.setPadding(new Insets(6, 14, 6, 14));
    sslBadge.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 12px;");

    Label lockIcon = new Label("🛡");
    lockIcon.setStyle("-fx-font-size: 11px;");
    Label sslLabel = new Label("Acceso Seguro SSL • RBAC Docente");
    sslLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #334155;");
    sslBadge.getChildren().addAll(lockIcon, sslLabel);

    Label versionLabel = new Label("Sistema de Información Académica v2.4");
    versionLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");

    Label copyrightLabel = new Label("© 2024 Universidad • Todos los derechos reservados");
    copyrightLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

    VBox footerBox = new VBox(6, sslBadge, versionLabel, copyrightLabel);
    footerBox.setAlignment(Pos.CENTER);

    mainContainer.getChildren().addAll(headerBox, progressBox);

    Region spacerTop = new Region();
    Region spacerBottom = new Region();
    VBox.setVgrow(spacerTop, Priority.ALWAYS);
    VBox.setVgrow(spacerBottom, Priority.ALWAYS);

    getChildren().addAll(spacerTop, mainContainer, spacerBottom, footerBox);
  }

  private void loadImage(ImageView logoView) {
    String[] possiblePaths = { "/images/logo.png", "/logo_splash.png", "/logo.png" };
    for (String path : possiblePaths) {
      try {
        InputStream is = getClass().getResourceAsStream(path);
        if (is != null) {
          logoView.setImage(new Image(is));
          return;
        }
      } catch (Exception ignored) {
      }
    }
  }

  private void startSequence(
      ApiClient api,
      TokenStore tokenStore,
      Consumer<SessionTokens> onSuccess,
      Runnable onLoginRequired) {

    long minDuration = AppConfig.splashMinMillis();
    int totalSteps = 100;
    long stepDurationMs = Math.max(10, minDuration / totalSteps);

    Timeline timeline = new Timeline(new KeyFrame(Duration.millis(stepDurationMs), event -> {
      progress += (1.0 / totalSteps);
      if (progress > 1.0)
        progress = 1.0;
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

      if (progress >= 1.0) {
        animationCompleted.set(true);
        tryCompleteNavigation();
      }
    }));
    timeline.setCycleCount(totalSteps);
    timeline.play();

    // Verificación de sesión en segundo plano
    if (api == null || tokenStore == null) {
      pendingNavigation.set(onLoginRequired != null ? onLoginRequired : () -> {
      });
      sessionChecked.set(true);
      return;
    }

    try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
      exec.submit(() -> {
        var saved = tokenStore.loadRefreshToken();
        if (saved.isEmpty()) {
          pendingNavigation.set(onLoginRequired != null ? onLoginRequired : () -> {
          });
          sessionChecked.set(true);
          Platform.runLater(this::tryCompleteNavigation);
          return;
        }
        try {
          SessionTokens tok = api.refresh(saved.get());
          UserSession.getInstance()
              .login(tok.accessToken(), tok.username(), tok.displayName(),
                  tok.groups(), tok.expiresAt());
          UserSession.getInstance().touch();
          tokenStore.saveRefreshToken(tok.refreshToken());
          pendingNavigation.set(onSuccess != null ? () -> onSuccess.accept(tok) : () -> {
          });
        } catch (ApiException e) {
          tokenStore.clear();
          pendingNavigation.set(onLoginRequired != null ? onLoginRequired : () -> {
          });
        } catch (Exception e) {
          pendingNavigation.set(onLoginRequired != null ? onLoginRequired : () -> {
          });
        } finally {
          sessionChecked.set(true);
          Platform.runLater(this::tryCompleteNavigation);
        }
      });
    }
  }

  private void tryCompleteNavigation() {
    if (animationCompleted.get() && sessionChecked.get()) {
      Runnable nav = pendingNavigation.get();
      if (nav != null) {
        nav.run();
      }
    }
  }

  /** Solo tests: cálculo del retardo restante sin dormir. */
  static long remainingMillis(long startMillis, long nowMillis, long minMillis) {
    return Math.max(0, minMillis - (nowMillis - startMillis));
  }
}

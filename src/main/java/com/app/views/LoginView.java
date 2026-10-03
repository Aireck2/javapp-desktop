package com.app.views;

import com.app.api.ApiClient;
import com.app.api.ApiException;
import com.app.api.dto.LoginRequest;
import com.app.api.dto.SessionTokens;
import com.app.auth.TokenStore;
import com.app.session.UserSession;
import atlantafx.base.theme.Styles;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Vista de Login moderna mobile-first para el Portal Académico.
 * Diseñada en código Java puro con AtlantaFX y acotada a 350px de ancho máximo.
 */
public class LoginView extends VBox {

    public LoginView(ApiClient api, TokenStore tokenStore, Consumer<SessionTokens> onSuccess) {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(24, 20, 20, 20));
        setStyle("-fx-background-color: #F8FAFC;");

        // Contenedor principal acotado a 350px
        VBox mainContainer = new VBox(16);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.setMaxWidth(350);
        mainContainer.setMinWidth(300);
        mainContainer.setPrefWidth(350);

        // --- 1. HEADER ---
        // Logo institucional con fallback seguro
        ImageView logoView = new ImageView();
        try {
            InputStream stream = getClass().getResourceAsStream("/images/logo.png");
            if (stream != null) {
                Image logo = new Image(stream);
                logoView.setImage(logo);
            } else {
                applyFallbackLogo(logoView);
            }
        } catch (Exception e) {
            applyFallbackLogo(logoView);
        }
        logoView.setFitWidth(84);
        logoView.setFitHeight(84);
        logoView.setPreserveRatio(true);

        // Badge / Pill azul suave
        HBox badge = new HBox(6);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(4, 14, 4, 14));
        badge.setStyle("-fx-background-color: #EEF2FF; -fx-background-radius: 20px;");

        Circle dot = new Circle(3, Color.web("#2563EB"));
        Label badgeLabel = new Label("Período Lectivo 2024-II • Acceso Institucional");
        badgeLabel.setStyle("-fx-text-fill: #1E40AF; -fx-font-size: 11px; -fx-font-weight: bold;");
        badge.getChildren().addAll(dot, badgeLabel);

        // Título y subtítulo
        Label titleLabel = new Label("Portal Académico");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        Label subtitleLabel = new Label("Plataforma de Gestión y Control de Asistencia Universitaria");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-text-alignment: center;");
        subtitleLabel.setWrapText(true);

        VBox headerBox = new VBox(8, badge, titleLabel, subtitleLabel);
        headerBox.setAlignment(Pos.CENTER);

        // --- 2. FORM CARD ---
        VBox formCard = new VBox(14);
        formCard.setPadding(new Insets(20));
        formCard.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-background-radius: 16px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 12, 0, 0, 4);"
        );

        // Campo Usuario
        Label userLabel = new Label("Usuario o Correo Institucional");
        userLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        TextField userField = new TextField();
        userField.setPromptText("ej. d.alvarez@universidad.edu.pe");
        userField.setPrefHeight(44);
        userField.setStyle(
                "-fx-background-color: #F1F5F9; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: transparent; " +
                "-fx-prompt-text-fill: #94A3B8; " +
                "-fx-font-size: 13px;"
        );

        VBox userBox = new VBox(6, userLabel, userField);

        // Campo Contraseña
        Label passLabel = new Label("Contraseña");
        passLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        PasswordField passField = new PasswordField();
        passField.setPromptText("••••••••••••");
        passField.setPrefHeight(44);
        passField.setStyle(
                "-fx-background-color: #F1F5F9; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: transparent; " +
                "-fx-prompt-text-fill: #94A3B8; " +
                "-fx-font-size: 13px;"
        );

        VBox passBox = new VBox(6, passLabel, passField);

        // CheckBox Mantener sesión
        CheckBox rememberMe = new CheckBox("Mantener sesión iniciada");
        rememberMe.setSelected(true);
        rememberMe.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        // Mensaje de Error
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #DC2626;");
        errorLabel.setWrapText(true);

        // Botón Iniciar Sesión
        Button btnLogin = new Button("➔ Iniciar Sesión");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setPrefHeight(46);
        btnLogin.getStyleClass().addAll(Styles.ACCENT, Styles.LARGE);
        btnLogin.setStyle(
                "-fx-background-color: #032B7A; " +
                "-fx-background-radius: 10px; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 14px; " +
                "-fx-cursor: hand;"
        );

        formCard.getChildren().addAll(userBox, passBox, rememberMe, errorLabel, btnLogin);

        // Demo hint
        Label hintLabel = new Label("Demo: admin/demo · docente/demo · alumno/demo");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        // Spacer para empujar el footer al fondo
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // --- 3. FOOTER LEGAL ---
        Label footerLegal = new Label("Dirección General de Tecnologías Académicas © 2024");
        footerLegal.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-text-alignment: center;");

        // Lógica de inicio de sesión
        Runnable doLogin = () -> {
            errorLabel.setText("");
            String username = userField.getText() != null ? userField.getText().trim() : "";
            String password = passField.getText() != null ? passField.getText() : "";

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Por favor ingrese usuario y contraseña");
                return;
            }

            btnLogin.setDisable(true);
            var req = new LoginRequest(username, password);

            try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
                exec.submit(() -> {
                    try {
                        SessionTokens tok = api.login(req);
                        UserSession.getInstance().login(
                                tok.accessToken(),
                                tok.username(),
                                tok.displayName(),
                                tok.groups(),
                                tok.expiresAt()
                        );
                        UserSession.getInstance().touch();

                        if (rememberMe.isSelected()) {
                            tokenStore.saveRefreshToken(tok.refreshToken());
                        } else {
                            tokenStore.clear();
                        }

                        Platform.runLater(() -> {
                            btnLogin.setDisable(false);
                            if (onSuccess != null) {
                                onSuccess.accept(tok);
                            }
                        });
                    } catch (ApiException e) {
                        Platform.runLater(() -> {
                            errorLabel.setText("Usuario o contraseña incorrectos");
                            btnLogin.setDisable(false);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            errorLabel.setText("Error de comunicación: " + e.getMessage());
                            btnLogin.setDisable(false);
                        });
                    }
                });
            }
        };

        btnLogin.setOnAction(e -> doLogin.run());
        passField.setOnAction(e -> doLogin.run());
        userField.setOnAction(e -> passField.requestFocus());

        mainContainer.getChildren().addAll(logoView, headerBox, formCard, hintLabel, spacer, footerLegal);
        VBox.setVgrow(mainContainer, Priority.ALWAYS);

        getChildren().add(mainContainer);
    }

    private void applyFallbackLogo(ImageView logoView) {
        logoView.setFitWidth(84);
        logoView.setFitHeight(84);
    }
}

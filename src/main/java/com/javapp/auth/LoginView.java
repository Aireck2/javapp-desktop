package com.javapp.auth;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.javapp.api.ApiClient;
import com.javapp.api.ApiException;
import com.javapp.api.dto.LoginRequest;
import com.javapp.api.dto.SessionTokens;
import com.javapp.session.UserSession;

import atlantafx.base.theme.Styles;
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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class LoginView extends VBox {

    public LoginView(ApiClient api, TokenStore tokenStore, Consumer<SessionTokens> onSuccess) {
        setAlignment(Pos.CENTER);
        setSpacing(20);
        setPadding(new Insets(30, 24, 20, 24));
        // setMaxWidth(380);
        setStyle("-fx-background-color: #F8FAFC;");

        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(350);
        container.setMinWidth(350);
        container.setPrefWidth(350);
        // LOGO
        ImageView logoView = new ImageView();
        try {
            Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png")));
            logoView.setImage(logo);
        } catch (Exception e) {
            logoView.setFitWidth(80);
            logoView.setFitHeight(80);
        }
        logoView.setFitWidth(84);
        logoView.setFitHeight(84);
        logoView.setPreserveRatio(true);

        // HEADER
        HBox badge = new HBox(6);
        badge.setAlignment(Pos.CENTER);
        badge.setPadding(new Insets(4, 14, 4, 14));
        badge.setStyle("-fx-background-color: #EEF2FF; -fx-background-radius: 20px;");

        Circle dot = new Circle(3, Color.web("#2563EB"));
        Label badgeLabel = new Label("Período 2026-II • Acceso Institucional");
        badgeLabel.setStyle("-fx-text-fill: #1E40AF; -fx-font-size: 11px; -fx-font-weight: bold;");
        badge.getChildren().addAll(dot, badgeLabel);

        Label titleLabel = new Label("Portal Académico");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        Label subtitleLabel = new Label("Plataforma de Gestión y Control de Asistencia Universitaria");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B; -fx-text-alignment: center;");
        subtitleLabel.setWrapText(true);

        VBox headerBox = new VBox(6, badge, titleLabel, subtitleLabel);
        headerBox.setAlignment(Pos.CENTER);

        VBox formCard = new VBox(16);
        formCard.setPadding(new Insets(24));
        formCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 12, 0, 0, 4);");

        // FORM LOGIN
        Label userLabel = new Label("Usuario o Correo Institucional");
        userLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        TextField userField = new TextField();
        userField.setPromptText("ej. d.alvarez@universidad.edu.pe");
        userField.setPrefHeight(44);
        userField.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 10px; " +
                "-fx-border-color: transparent; -fx-prompt-text-fill: #94A3B8;");

        VBox userBox = new VBox(6, userLabel, userField);

        Label passLabel = new Label("Contraseña");
        passLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

        PasswordField passField = new PasswordField();
        passField.setPromptText("••••••••••••");
        passField.setPrefHeight(44);
        passField.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 10px; " +
                "-fx-border-color: transparent; -fx-prompt-text-fill: #94A3B8;");

        VBox passBox = new VBox(6, passLabel, passField);

        var error = new Label("");

        // --- CHECKBOX: MANTENER SESIÓN ---
        CheckBox rememberMe = new CheckBox("Mantener sesión iniciada");
        rememberMe.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        Button btn = new Button("➔  Iniciar Sesión");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(46);
        btn.getStyleClass().addAll(Styles.ACCENT, Styles.LARGE);
        btn.setStyle("-fx-background-color: #032B7A; -fx-background-radius: 10px; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-cursor: hand;");

        formCard.getChildren().addAll(userBox, passBox, rememberMe, btn);

        var hint = new Label("Demo: admin/demo · docente/demo · alumno/demo");
        hint.setStyle("-fx-opacity: 0.7;");

        // Footer
        Label footerLabel = new Label("javapp desktop © 2026");
        footerLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Runnable doLogin = () -> {
            error.setText("");
            btn.setDisable(true);
            var req = new LoginRequest(userField.getText(), passField.getText());
            if (req.username().isEmpty() || req.password().isEmpty()) {
                error.setText("Ingrese usuario y contraseña");
                btn.setDisable(false);
                return;
            }
            try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
                exec.submit(() -> {
                    try {
                        SessionTokens tok = api.login(req);
                        UserSession.getInstance()
                                .login(tok.accessToken(), tok.username(), tok.displayName(),
                                        tok.groups(), tok.expiresAt());
                        UserSession.getInstance().touch();
                        tokenStore.saveRefreshToken(tok.refreshToken());
                        Platform.runLater(() -> onSuccess.accept(tok));
                    } catch (ApiException e) {
                        Platform.runLater(() -> {
                            // US-01 CA5: mensaje genérico
                            error.setText("Usuario o contraseña inválidos");
                            btn.setDisable(false);
                        });
                    } finally {
                        Platform.runLater(() -> btn.setDisable(false));
                    }
                });
            }
        };
        btn.setOnAction(e -> doLogin.run());

        container.getChildren().addAll(logoView, headerBox, formCard, error, hint, spacer, footerLabel);

        getChildren().addAll(container);
    }
}

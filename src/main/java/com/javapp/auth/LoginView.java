package com.javapp.auth;

import com.javapp.api.ApiClient;
import com.javapp.api.ApiException;
import com.javapp.api.dto.LoginRequest;
import com.javapp.api.dto.SessionTokens;
import com.javapp.session.UserSession;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Login US-01: usuario/correo + contraseña, error genérico, demo admin|docente|alumno / demo. */
public class LoginView extends VBox {

    public LoginView(ApiClient api, TokenStore tokenStore, Consumer<SessionTokens> onSuccess) {
        super(10);
        setPadding(new Insets(32));
        setAlignment(Pos.CENTER);
        setMaxWidth(380);

        var title = new Label("Javapp · Iniciar sesión");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        var user = new TextField();
        user.setPromptText("Usuario o correo (admin, docente, alumno)");
        var pass = new PasswordField();
        pass.setPromptText("Contraseña (demo)");
        var error = new Label();
        error.setStyle("-fx-text-fill: -color-danger-emphasis;");
        error.setWrapText(true);
        var btn = new Button("Entrar");
        btn.setDefaultButton(true);
        btn.setMaxWidth(Double.MAX_VALUE);
        var hint = new Label("Demo: admin/demo · docente/demo · alumno/demo");
        hint.setStyle("-fx-opacity: 0.7;");

        Runnable doLogin = () -> {
            error.setText("");
            btn.setDisable(true);
            var req = new LoginRequest(user.getText(), pass.getText());
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
        getChildren().addAll(title, user, pass, btn, error, hint);
    }
}

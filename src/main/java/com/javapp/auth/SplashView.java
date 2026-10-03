package com.javapp.auth;

import com.javapp.api.ApiClient;
import com.javapp.api.ApiException;
import com.javapp.api.dto.SessionTokens;
import com.javapp.config.AppConfig;
import com.javapp.session.UserSession;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;

/**
 * Vista Splash / Bienvenida (mvp.md §2.1, US-01).
 * Consulta el refresh token guardado y renueva sesión en segundo plano:
 * 200 OK → router por rol; 401/ausente → Login.
 */
public class SplashView extends VBox {

    public SplashView(
            ApiClient api,
            TokenStore tokenStore,
            Consumer<SessionTokens> onSuccess,
            Runnable onLoginRequired) {
        super(12);
        setPadding(new Insets(32));
        setAlignment(Pos.CENTER);

        var title = new Label("Javapp · Bienvenido");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        var status = new Label("Comprobando sesión guardada…");
        status.setWrapText(true);
        var spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        getChildren().addAll(title, spinner, status);

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            exec.submit(() -> {
                long start = System.currentTimeMillis();
                var saved = tokenStore.loadRefreshToken();
                if (saved.isEmpty()) {
                    settle(start, onLoginRequired);
                    return;
                }
                try {
                    SessionTokens tok = api.refresh(saved.get());
                    UserSession.getInstance()
                            .login(tok.accessToken(), tok.username(), tok.displayName(),
                                    tok.groups(), tok.expiresAt());
                    UserSession.getInstance().touch();
                    tokenStore.saveRefreshToken(tok.refreshToken());
                    settle(start, () -> onSuccess.accept(tok));
                } catch (ApiException e) {
                    tokenStore.clear();
                    settle(start, onLoginRequired);
                }
            });
        }
    }

    /**
     * Garantiza el tiempo mínimo visible del Splash antes de navegar, para que la
     * bienvenida se perciba aunque el refresh responda al instante.
     */
    private static void settle(long startMillis, Runnable navigate) {
        long remaining = AppConfig.splashMinMillis() - (System.currentTimeMillis() - startMillis);
        if (remaining > 0) {
            try {
                Thread.sleep(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Platform.runLater(navigate);
    }

    /** Solo tests: cálculo del retardo restante sin dormir. */
    static long remainingMillis(long startMillis, long nowMillis, long minMillis) {
        return Math.max(0, minMillis - (nowMillis - startMillis));
    }
}

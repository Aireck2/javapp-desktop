package com.javapp;

import atlantafx.base.theme.PrimerDark;
import com.javapp.api.ApiClient;
import com.javapp.api.MockApiClient;
import com.javapp.attendance.AttendanceMatrixView;
import com.javapp.attendance.TakeAttendanceView;
import com.javapp.auth.LoginView;
import com.javapp.auth.TokenStore;
import com.javapp.config.AppConfig;
import com.javapp.dashboard.StudentDashboardView;
import com.javapp.session.UserSession;
import java.time.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Shell JavaFX: tema AtlantaFX, router por rol (US-01), control inactividad. */
public class App extends Application {

    private static final ApiClient API = new MockApiClient();
    private static final TokenStore TOKENS = new TokenStore();

    @Override
    public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        stage.setTitle("Javapp Desktop · MVP");
        showLogin(stage);
        watchInactivity(stage);
        stage.setWidth(1024);
        stage.setHeight(720);
        stage.show();
    }

    private void showLogin(Stage stage) {
        var root = new StackPane(new LoginView(API, TOKENS, tok -> showMain(stage)));
        root.setPadding(new Insets(16));
        stage.setScene(new Scene(root));
    }

    private void showMain(Stage stage) {
        var session = UserSession.getInstance();
        var content = new StackPane();
        content.setPadding(new Insets(8));

        var nav = new HBox(8);
        nav.setPadding(new Insets(8));
        boolean isAdmin = session.hasRole("ADMIN");
        boolean isDocente = session.hasRole("DOCENTE");
        boolean isAlumno = session.hasRole("ALUMNO");

        if (isAdmin || isDocente) {
            var bAsis = new Button("Tomar asistencia");
            bAsis.setOnAction(e -> content.getChildren().setAll(new TakeAttendanceView(API)));
            var bMat = new Button("Histórico");
            bMat.setOnAction(e -> content.getChildren().setAll(new AttendanceMatrixView(API)));
            nav.getChildren().addAll(bAsis, bMat);
        }
        if (isAdmin || isDocente || isAlumno) {
            var bDash = new Button(isAlumno ? "Mi asistencia" : "Panel alumno (demo)");
            bDash.setOnAction(e -> content.getChildren()
                    .setAll(new StudentDashboardView(API, isAlumno ? session.getUsername().equals("alumno") ? "alu-01" : "alu-01" : "alu-01")));
            nav.getChildren().add(bDash);
        }
        if (isAdmin) {
            nav.getChildren().add(new Label("ADMIN · acceso global (MVP: módulos de asistencia)"));
        }
        var user = new Label(session.getDisplayName() + " " + session.getGroups());
        var out = new Button("Salir");
        out.setOnAction(e -> {
            session.logout();
            TOKENS.clear();
            showLogin(stage);
        });
        var top = new HBox(10, nav, user, out);

        // Vista inicial por rol
        if (isAlumno || (!isAdmin && !isDocente)) {
            content.getChildren().setAll(new StudentDashboardView(API, "alu-01"));
        } else {
            content.getChildren().setAll(new TakeAttendanceView(API));
        }

        var root = new BorderPane(content, new VBox(top), null, null, null);
        stage.setScene(new Scene(root));
    }

    /** US-01 CA6: expiración por inactividad (default 15 min, chequeo cada 60 s). */
    private void watchInactivity(Stage stage) {
        var timer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(60), e -> {
            var s = UserSession.getInstance();
            if (s.isLoggedIn() && s.isInactive(Duration.ofMinutes(AppConfig.inactivityMinutes()))) {
                s.logout();
                TOKENS.clear();
                showLogin(stage);
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
        stage.sceneProperty().addListener((o, a, scene) -> {
            if (scene != null) {
                scene.addEventFilter(javafx.scene.input.InputEvent.ANY, ev -> UserSession.getInstance().touch());
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package com.app.layout;

import com.app.api.ApiClient;
import com.app.attendance.AttendanceMatrixView;
import com.app.attendance.CourseDetailView;
import com.app.attendance.MyCoursesView;
import com.app.attendance.TakeAttendanceView;
import com.app.dashboard.StudentDashboardView;
import com.app.navigation.ScreenRouter;
import com.app.session.UserSession;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Layout base tipo Shell para pantallas internas tras autenticación.
 * Contiene una barra superior con información del usuario, un área central
 * gestionada por un ScreenRouter interno y una barra de navegación inferior (Bottom Bar).
 */
public class MainShell extends BorderPane {

    private final ApiClient api;
    private final ScreenRouter innerRouter;
    private final StackPane contentArea;
    private final Runnable onLogout;

    public MainShell(ApiClient api, Runnable onLogout) {
        this.api = api;
        this.onLogout = onLogout;

        setStyle("-fx-background-color: #F8FAFC;");

        // Área central de vistas internas
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: transparent;");
        innerRouter = new ScreenRouter(contentArea);

        // Header interno
        Node header = createHeader();
        setTop(header);

        // Contenido central
        setCenter(contentArea);

        // Bottom Navigation Bar
        Node bottomBar = createBottomNavigationBar();
        setBottom(bottomBar);

        // Registrar rutas internas
        registerInnerRoutes();

        // Navegar a la vista inicial según rol
        navigateInitialView();
    }

    private Node createHeader() {
        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(10, 16, 10, 16));
        headerBox.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

        UserSession session = UserSession.getInstance();
        VBox userBox = new VBox(2);
        Label userLabel = new Label(session.getDisplayName());
        userLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        String roleDesc = session.getGroups().isEmpty() ? "Usuario" : String.join(", ", session.getGroups());
        Label roleLabel = new Label(roleDesc);
        roleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        userBox.getChildren().addAll(userLabel, roleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Salir");
        logoutBtn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            session.logout();
            if (onLogout != null) {
                onLogout.run();
            }
        });

        headerBox.getChildren().addAll(userBox, spacer, logoutBtn);
        return headerBox;
    }

    private Node createBottomNavigationBar() {
        HBox nav = new HBox(6);
        nav.setAlignment(Pos.CENTER);
        nav.setPadding(new Insets(8, 12, 12, 12));
        nav.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 1 0 0 0;");

        UserSession session = UserSession.getInstance();
        boolean isAdmin = session.hasRole("ADMIN");
        boolean isDocente = session.hasRole("DOCENTE");
        boolean isAlumno = session.hasRole("ALUMNO");

        if (isAdmin || isDocente) {
            Button btnCursos = createNavButton("📚 Cursos", () -> innerRouter.navigateTo("courses"));
            nav.getChildren().add(btnCursos);

            if (isAdmin) {
                Button btnAsistencia = createNavButton("✍ Asistencia", () -> innerRouter.navigateTo("take-attendance"));
                Button btnHistorico = createNavButton("📊 Histórico", () -> innerRouter.navigateTo("matrix"));
                nav.getChildren().addAll(btnAsistencia, btnHistorico);
            }
        }

        if (isAdmin || isAlumno) {
            String title = isAlumno ? "📊 Mi Asistencia" : "👤 Panel Alumno";
            Button btnAlumno = createNavButton(title, () -> innerRouter.navigateTo("student-dashboard"));
            nav.getChildren().add(btnAlumno);
        }

        return nav;
    }

    private Button createNavButton(String text, Runnable action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btn, Priority.ALWAYS);
        btn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #334155; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-cursor: hand; -fx-padding: 8 4 8 4;");
        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void registerInnerRoutes() {
        innerRouter.registerView("courses", data -> {
            Consumer<com.app.api.dto.ClassSession> goToDetail = cls -> innerRouter.navigateTo("course-detail", cls);
            return new MyCoursesView(api, goToDetail);
        });

        innerRouter.registerView("course-detail", data -> {
            if (data instanceof com.app.api.dto.ClassSession cls) {
                return new CourseDetailView(api, cls, innerRouter::goBack);
            }
            return new Label("Sesión no especificada");
        });

        innerRouter.registerView("take-attendance", data -> new TakeAttendanceView(api));
        innerRouter.registerView("matrix", data -> new AttendanceMatrixView(api));
        innerRouter.registerView("student-dashboard", data -> {
            UserSession session = UserSession.getInstance();
            String studentId = session.getUsername().equals("alumno") ? "alu-01" : "alu-01";
            return new StudentDashboardView(api, studentId);
        });
    }

    private void navigateInitialView() {
        UserSession session = UserSession.getInstance();
        if (session.hasRole("DOCENTE") && !session.hasRole("ADMIN")) {
            innerRouter.navigateTo("courses");
        } else if (session.hasRole("ALUMNO") || (!session.hasRole("ADMIN") && !session.hasRole("DOCENTE"))) {
            innerRouter.navigateTo("student-dashboard");
        } else {
            innerRouter.navigateTo("take-attendance");
        }
    }

    public ScreenRouter getInnerRouter() {
        return innerRouter;
    }
}

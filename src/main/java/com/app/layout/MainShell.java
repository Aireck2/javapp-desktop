package com.app.layout;

import java.util.function.Consumer;

import com.app.api.ApiClient;
import com.app.attendance.AttendanceMatrixView;
import com.app.attendance.CourseDetailView;
import com.app.attendance.MyCoursesView;
import com.app.attendance.TakeAttendanceView;
import com.app.components.BottomNavBar;
import com.app.dashboard.StudentDashboardView;
import com.app.navigation.ScreenRouter;
import com.app.session.UserSession;
import com.app.views.ProfileView;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Shell principal de la aplicación que extiende {@link SharedPortalShell}.
 * Provee la cabecera compartida (AppHeader), el contenedor central scrolleable
 * con ScreenRouter
 * y la barra de navegación inferior (BottomNavBar) compartida entre Docente y
 * Alumno.
 */
public class MainShell extends SharedPortalShell {

    private final ApiClient api;
    private final ScreenRouter innerRouter;
    private final StackPane contentArea;
    private final Runnable onLogout;

    public MainShell(ApiClient api, Runnable onLogout) {
        this(api, onLogout, resolvePortalTitle(), resolveIsTeacher());
    }

    public MainShell(ApiClient api, Runnable onLogout, String portalTypeTitle, boolean isTeacher) {
        super(portalTypeTitle, isTeacher, () -> handleLogout(onLogout), BottomNavBar.TabItem.ATTENDANCE);
        this.api = api;
        this.onLogout = onLogout;

        // Área central administrada por el enrutador
        this.contentArea = new StackPane();
        this.contentArea.setStyle("-fx-background-color: transparent;");
        this.innerRouter = new ScreenRouter(contentArea);

        // Se inyecta el contenedor del router en el ScrollPane del SharedPortalShell
        setViewContent(contentArea);

        // Conectar interacción de navegación del BottomNavBar
        getBottomNavBar().setOnTabSelected(this::handleTabSelection);

        // Registrar rutas internas
        registerInnerRoutes();

        // Navegar a la pantalla de inicio según el rol
        navigateInitialView();
    }

    private void handleTabSelection(BottomNavBar.TabItem tab) {
        UserSession session = UserSession.getInstance();
        boolean isDocente = session.hasRole("DOCENTE");
        boolean isAlumno = session.hasRole("ALUMNO");
        boolean isAdmin = session.hasRole("ADMIN");

        switch (tab) {
            case ATTENDANCE -> {
                if (isDocente) {
                    innerRouter.navigateTo("courses");
                } else if (isAlumno) {
                    innerRouter.navigateTo("student-dashboard");
                } else {
                    innerRouter.navigateTo("student-dashboard");
                }
            }

            case SCHEDULE -> {
                if (isDocente) {
                    innerRouter.navigateTo("courses");
                } else {
                    innerRouter.navigateTo("student-dashboard");
                }
            }
            case PROFILE -> innerRouter.navigateTo("profile");
        }
    }

    private static void handleLogout(Runnable onLogout) {
        UserSession.getInstance().logout();
        if (onLogout != null) {
            onLogout.run();
        }
    }

    private void registerInnerRoutes() {
        innerRouter.registerView("courses", data -> {
            Consumer<com.app.api.dto.ClassSession> goToDetail = cls -> innerRouter.navigateTo("course-detail", cls);
            Consumer<com.app.api.dto.ClassSession> goToAttendance = cls -> innerRouter.navigateTo("take-attendance",
                    cls);
            return new MyCoursesView(api, goToDetail, goToAttendance);
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
            String studentId = session.getUsername() != null && !session.getUsername().isBlank()
                    ? session.getUsername()
                    : "alu-01";
            return new StudentDashboardView(api, studentId);
        });
        innerRouter.registerView("profile", data -> new ProfileView());
    }

    private void navigateInitialView() {
        UserSession session = UserSession.getInstance();
        if (session.hasRole("DOCENTE") && !session.hasRole("ADMIN")) {
            innerRouter.navigateTo("courses");
            getBottomNavBar().setActiveTab(BottomNavBar.TabItem.ATTENDANCE);
        } else if (session.hasRole("ALUMNO") || (!session.hasRole("ADMIN") && !session.hasRole("DOCENTE"))) {
            innerRouter.navigateTo("student-dashboard");
            getBottomNavBar().setActiveTab(BottomNavBar.TabItem.ATTENDANCE);
        } else {
            innerRouter.navigateTo("take-attendance");
            getBottomNavBar().setActiveTab(BottomNavBar.TabItem.ATTENDANCE);
        }
    }

    private static String resolvePortalTitle() {
        UserSession session = UserSession.getInstance();
        if (session.hasRole("DOCENTE")) {
            return "PORTAL DOCENTE";
        } else if (session.hasRole("ALUMNO")) {
            return "PORTAL ALUMNO";
        } else if (session.hasRole("ADMIN")) {
            return "PORTAL ADMINISTRADOR";
        }
        return "PORTAL ACADÉMICO";
    }

    private static boolean resolveIsTeacher() {
        UserSession session = UserSession.getInstance();
        return session.hasRole("DOCENTE") || session.hasRole("ADMIN");
    }

    public ScreenRouter getInnerRouter() {
        return innerRouter;
    }
}

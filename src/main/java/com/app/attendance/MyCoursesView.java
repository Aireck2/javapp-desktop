package com.app.attendance;

import com.app.api.ApiClient;
import com.app.api.ApiException;
import com.app.api.dto.ClassSession;
import com.app.api.dto.Course;
import com.app.components.CourseCard;
import com.app.mappers.CourseModelMapper;
import com.app.models.CourseModel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Vista "Mis Cursos" del Portal Docente.
 * Muestra las sesiones y cursos agrupados cronológicamente (Hoy … +5 días)
 * utilizando tarjetas interactivas {@link CourseCard} construidas a partir de {@link CourseModel}.
 */
public class MyCoursesView extends VBox {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEEE dd/MM", new Locale("es"));

    private final ApiClient api;
    private final Consumer<ClassSession> onDetail;
    private final Consumer<ClassSession> onTakeAttendance;

    public MyCoursesView(ApiClient api, Consumer<ClassSession> onDetail) {
        this(api, onDetail, onDetail);
    }

    public MyCoursesView(
            ApiClient api,
            Consumer<ClassSession> onDetail,
            Consumer<ClassSession> onTakeAttendance) {

        super(16);
        this.api = api;
        this.onDetail = onDetail;
        this.onTakeAttendance = onTakeAttendance;

        setPadding(new Insets(16));
        setStyle("-fx-background-color: transparent;");

        buildView();
    }

    private void buildView() {
        getChildren().clear();

        Label pageTitle = new Label("Mis Cursos");
        pageTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");

        Label pageSubtitle = new Label("Próximas sesiones programadas (Hoy … +5 días)");
        pageSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        VBox titleBox = new VBox(2, pageTitle, pageSubtitle);
        getChildren().add(titleBox);

        try {
            LocalDate today = LocalDate.now();
            List<ClassSession> windowSessions = api.sessionsWindow(today, today.plusDays(5));
            List<Course> coursesList = api.coursesForCurrentUser();
            Map<String, Course> coursesMap = coursesList.stream()
                    .collect(Collectors.toMap(Course::id, c -> c, (a, b) -> a));

            // Cache de alumnos matriculados por curso
            Map<String, Integer> enrolledMap = new HashMap<>();
            for (Course c : coursesList) {
                try {
                    enrolledMap.put(c.id(), api.studentsByCourse(c.id()).size());
                } catch (ApiException ignored) {
                    enrolledMap.put(c.id(), 0);
                }
            }

            if (windowSessions.isEmpty()) {
                VBox emptyBox = createEmptyState("Sin sesiones próximas en los siguientes 5 días.");
                getChildren().add(emptyBox);
                return;
            }

            Map<LocalDate, List<ClassSession>> byDate = windowSessions.stream()
                    .collect(Collectors.groupingBy(ClassSession::date, TreeMap::new, Collectors.toList()));

            VBox timelineContainer = new VBox(18);

            for (int i = 0; i <= 5; i++) {
                LocalDate date = today.plusDays(i);
                List<ClassSession> daySessions = byDate.getOrDefault(date, List.of());

                VBox daySection = new VBox(10);
                Label sectionHeader = new Label(formatSectionTitle(date, i == 0));
                sectionHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + (i == 0 ? "#064E3B" : "#334155") + ";");
                daySection.getChildren().add(sectionHeader);

                if (daySessions.isEmpty()) {
                    Label noSessionLabel = new Label(i == 0 ? "Sin sesión programada para hoy." : "Sin sesiones este día.");
                    noSessionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-padding: 4 0 4 8;");
                    daySection.getChildren().add(noSessionLabel);
                } else {
                    for (ClassSession session : daySessions) {
                        Course course = coursesMap.get(session.courseId());
                        int enrolled = enrolledMap.getOrDefault(session.courseId(), 0);
                        int completedHours = session.blockHours() * 4; // Horas estimadas/acumuladas

                        CourseModel model = CourseModelMapper.toModel(course, session, enrolled, completedHours);

                        CourseCard card = new CourseCard(model, true, m -> {
                            if ("Clase Hoy".equalsIgnoreCase(m.badgeType()) && onTakeAttendance != null) {
                                onTakeAttendance.accept(session);
                            } else if (onDetail != null) {
                                onDetail.accept(session);
                            }
                        });

                        daySection.getChildren().add(card);
                    }
                }

                timelineContainer.getChildren().add(daySection);
            }

            getChildren().add(timelineContainer);

        } catch (ApiException e) {
            Label errorLabel = new Label("No fue posible cargar las sesiones: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-padding: 12;");
            getChildren().add(errorLabel);
        }
    }

    private static String formatSectionTitle(LocalDate date, boolean isToday) {
        String base = date.format(DATE_FORMATTER);
        if (base != null && !base.isEmpty()) {
            base = base.substring(0, 1).toUpperCase(new Locale("es")) + base.substring(1);
        }
        return (isToday ? "📌 HOY · " : "📅 ") + base;
    }

    private VBox createEmptyState(String message) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(32, 16, 32, 16));

        Label label = new Label(message);
        label.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

        box.getChildren().add(label);
        return box;
    }
}

package com.app.attendance;

import com.app.api.ApiClient;
import com.app.api.ApiException;
import com.app.api.dto.ClassSession;
import com.app.api.dto.Course;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/**
 * Teacher role view 1 — "Mis cursos": sessions grouped by date (today …
 * today+5), with the date as section title. No session today → "Sin sesión
 * hoy".
 */
public class MyCoursesView extends VBox {

    private static final DateTimeFormatter TITLE =
            DateTimeFormatter.ofPattern("EEEE dd/MM", new Locale("es"));

    public MyCoursesView(ApiClient api, Consumer<ClassSession> onDetail) {
        super(10);
        setPadding(new Insets(16));

        var title = new Label("Mis cursos");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        getChildren().add(title);

        var status = new Label();
        status.setWrapText(true);

        var sections = new VBox(12);
        var scroll = new ScrollPane(sections);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(560);
        getChildren().addAll(scroll, status);

        try {
            LocalDate today = LocalDate.now();
            List<ClassSession> window = api.sessionsWindow(today, today.plusDays(5));
            Map<String, Course> courses = api.coursesForCurrentUser().stream()
                    .collect(Collectors.toMap(Course::id, c -> c));
            Map<LocalDate, List<ClassSession>> byDate = window.stream()
                    .collect(Collectors.groupingBy(ClassSession::date, TreeMap::new, Collectors.toList()));

            if (window.isEmpty()) {
                sections.getChildren().add(new Label("Sin sesiones próximas (hoy … +5 días)."));
                return;
            }
            for (int i = 0; i <= 5; i++) {
                LocalDate date = today.plusDays(i);
                var header = new Label(sectionTitle(date, i == 0));
                header.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
                var box = new VBox(8);
                box.getChildren().add(header);
                List<ClassSession> daySessions = byDate.getOrDefault(date, List.of());
                if (daySessions.isEmpty()) {
                    box.getChildren().add(new Label(i == 0 ? "Sin sesión hoy." : "Sin sesiones este día."));
                }
                for (ClassSession s : daySessions) {
                    box.getChildren().add(card(api, courses.get(s.courseId()), s, onDetail));
                }
                sections.getChildren().add(box);
            }
            status.setText("Ventana hoy … +5 días. Pulse «Ir a detalle» para abrir el curso.");
        } catch (ApiException e) {
            status.setText("Error: " + e.getMessage());
        }
    }

    private static String sectionTitle(LocalDate date, boolean isToday) {
        String base = date.format(TITLE);
        base = base.substring(0, 1).toUpperCase(new Locale("es")) + base.substring(1);
        return (isToday ? "Hoy · " : "") + base;
    }

    private static VBox card(
            ApiClient api, Course course, ClassSession session, Consumer<ClassSession> onDetail) {
        var box = new VBox(4);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-border-color: #4a4a4a; -fx-border-radius: 6; -fx-background-radius: 6;");
        String name = course == null ? session.courseId() : course.name();
        String code = course == null ? "" : course.code();
        String section = course == null ? "" : course.section();
        String schedule = course == null ? "" : course.schedule();
        int enrolled = 0;
        try {
            enrolled = api.studentsByCourse(session.courseId()).size();
        } catch (ApiException ignored) {
        }
        var l1 = new Label(code + " · " + name + " · " + section);
        l1.setStyle("-fx-font-weight: bold;");
        l1.setWrapText(true);
        var l2 = new Label(schedule + " · " + session.blockHours() + "h · " + session.status()
                + " · Matriculados: " + enrolled);
        l2.setWrapText(true);
        var btn = new Button("Ir a detalle");
        btn.setOnAction(e -> onDetail.accept(session));
        box.getChildren().addAll(l1, l2, btn);
        return box;
    }
}

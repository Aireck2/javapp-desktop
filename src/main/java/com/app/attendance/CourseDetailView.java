package com.app.attendance;

import com.app.api.ApiClient;
import com.app.api.ApiException;
import com.app.api.dto.ClassSession;
import com.app.api.dto.Course;
import com.app.api.dto.Student;
import com.app.api.dto.StudentSummary;
import com.app.common.AttendanceExport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Teacher role view 2 — "Curso {name}": course header + roster with % and risk
 * semaphore per row, N checkboxes per hour (checked=present), save → DICTADA
 * (US-08), draft autosave US-09 and xlsx export US-14. Future session →
 * read-only.
 */
public class CourseDetailView extends VBox {

    /** In-memory draft US-09: sessionId → same-day marks (restored when reopened today). */
    private record Draft(LocalDate date, Map<String, boolean[]> marks) {}

    private static final Map<String, Draft> DRAFTS = new ConcurrentHashMap<>();

    public CourseDetailView(ApiClient api, ClassSession session, Runnable onBack) {
        super(10);
        setPadding(new Insets(16));

        Course course = api.coursesForCurrentUser().stream()
                .filter(c -> c.id().equals(session.courseId()))
                .findFirst()
                .orElseGet(() -> new Course(session.courseId(), session.courseId(), session.courseId(), 0));

        var top = new HBox(10);
        var back = new Button("← Mis cursos");
        back.setOnAction(e -> onBack.run());
        var title = new Label("Curso " + course.name());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        top.getChildren().addAll(back, title);

        List<Student> roster;
        try {
            roster = api.studentsByCourse(course.id());
        } catch (ApiException e) {
            roster = List.of();
        }
        var header = new Label(course.name() + " · " + course.code() + " · " + course.section()
                + " · " + course.schedule() + " · Docente: " + course.teacher()
                + " · Matriculados: " + roster.size()
                + " · Sesión: " + session.date() + " · " + session.blockHours() + "h · " + session.status());
        header.setWrapText(true);
        header.setStyle("-fx-font-weight: bold;");

        var status = new Label();
        status.setWrapText(true);

        boolean future = session.date().isAfter(LocalDate.now());
        var notice = new Label();
        notice.setWrapText(true);
        if (future) {
            notice.setText("Sesión futura, aún no editable (US-08: sin fechas futuras). Solo lectura.");
        }

        var rows = new VBox(8);
        Map<String, List<CheckBox>> boxes = new HashMap<>();
        Draft draft = DRAFTS.get(session.id());
        boolean hasDraft = !future && draft != null && draft.date().equals(LocalDate.now());

        for (Student student : roster) {
            var row = new HBox(8);
            StudentSummary summary;
            try {
                summary = api.studentSummary(course.id(), student.id());
            } catch (ApiException e) {
                summary = null;
            }
            String info = summary == null
                    ? student.code() + " · " + student.fullName()
                    : String.format("%s · %s · %.0fh asist · %.0fh faltas · %.0f%% · %s (margen %.0fh)",
                            student.code(), student.fullName(), summary.attendedHours(), summary.missedHours(),
                            summary.percentage(), summary.risk(), summary.remainingHours());
            var infoLabel = new Label(info);
            infoLabel.setMinWidth(420);
            infoLabel.setWrapText(true);
            row.getChildren().add(infoLabel);
            List<CheckBox> cbs = new ArrayList<>();
            boolean[] init = hasDraft ? draft.marks().get(student.id()) : null;
            for (int i = 0; i < session.blockHours(); i++) {
                var cb = new CheckBox("H" + (i + 1));
                cb.setSelected(init != null && i < init.length ? init[i] : true);
                cb.setDisable(future);
                cbs.add(cb);
                row.getChildren().add(cb);
            }
            boxes.put(student.id(), cbs);
            rows.getChildren().add(row);
        }
        if (hasDraft) {
            status.setText("Borrador recuperado de hoy (US-09).");
        }

        var scroll = new ScrollPane(rows);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(380);

        // US-09: draft autosave every 60 s (non-future sessions only)
        var dirty = new boolean[] {false};
        Timeline autosave = null;
        if (!future) {
            boxes.values().forEach(cbs -> cbs.forEach(cb ->
                    cb.selectedProperty().addListener((ob, ov, nv) -> dirty[0] = true)));
            autosave = new Timeline(new KeyFrame(javafx.util.Duration.seconds(60), ev -> {
                if (!dirty[0]) {
                    return;
                }
                DRAFTS.put(session.id(), new Draft(LocalDate.now(), snapshot(boxes)));
                dirty[0] = false;
                status.setText("Borrador autoguardado "
                        + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                        + " (US-09, editable hasta 23:59).");
            }));
            autosave.setCycleCount(Timeline.INDEFINITE);
            autosave.play();
        }
        final Timeline autosaveFinal = autosave;

        var actions = new HBox(10);
        var save = new Button("Guardar asistencia");
        save.setDisable(future);
        save.setOnAction(e -> {
            if (session.date().isAfter(LocalDate.now())) {
                new Alert(Alert.AlertType.ERROR, "No se permite tomar asistencia en fechas futuras.").showAndWait();
                return;
            }
            try {
                api.saveAttendance(session.id(), snapshot(boxes));
                DRAFTS.remove(session.id());
                if (autosaveFinal != null) {
                    autosaveFinal.stop();
                }
                status.setText("Guardado " + LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                        + ". Sesión " + session.id() + " → DICTADA. % y margen recalculados.");
            } catch (ApiException ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
            }
        });

        var btnExport = new Button("Exportar Listado a Excel (.xlsx)");
        btnExport.setOnAction(e -> {
            var fc = new FileChooser();
            fc.setInitialFileName("asistencia-" + course.code() + ".xlsx");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
            var file = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
            if (file == null) {
                return;
            }
            try {
                List<ClassSession> sessions = api.validSessions(course.id());
                List<AttendanceExport.StudentRow> exportRows = new ArrayList<>();
                for (Student student : api.studentsByCourse(course.id())) {
                    StudentSummary summary = api.studentSummary(course.id(), student.id());
                    exportRows.add(new AttendanceExport.StudentRow(
                            student.code(), student.fullName(), summary.attendedHours(), summary.missedHours(),
                            summary.percentage(), summary.risk().name()));
                }
                AttendanceExport.exportMatriz(course.code(), sessions, exportRows, file);
                status.setText("Exportado a " + file.getName() + " (US-14).");
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Export falló: " + ex.getMessage()).showAndWait();
            }
        });
        actions.getChildren().addAll(save, btnExport);

        getChildren().addAll(top, header, notice, scroll, actions, status);
    }

    private static Map<String, boolean[]> snapshot(Map<String, List<CheckBox>> boxes) {
        Map<String, boolean[]> payload = new HashMap<>();
        boxes.forEach((student, cbs) -> {
            boolean[] arr = new boolean[cbs.size()];
            for (int i = 0; i < cbs.size(); i++) {
                arr[i] = cbs.get(i).isSelected();
            }
            payload.put(student, arr);
        });
        return payload;
    }
}

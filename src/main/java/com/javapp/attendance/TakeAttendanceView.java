package com.javapp.attendance;

import com.javapp.api.ApiClient;
import com.javapp.api.ApiException;
import com.javapp.api.dto.ClassSession;
import com.javapp.api.dto.ClassSession.SessionStatus;
import com.javapp.api.dto.Course;
import com.javapp.api.dto.StudentSummary;
import com.javapp.common.AttendanceExport;
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
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * US-08 + mvp.md §4 (unified view): general header, roster with % and risk
 * semaphore per row, N checkboxes per hour (checked=present), save → DICTADA,
 * draft autosave US-09 and xlsx export US-14. Admin use; teachers use
 * {@link MyCoursesView} + {@link CourseDetailView}.
 */
public class TakeAttendanceView extends VBox {

    private static final List<String> STUDENTS = List.of("alu-01", "alu-02", "alu-03");

    /** In-memory draft US-09: sessionId → same-day marks (restored when reopened today). */
    private record Draft(LocalDate date, Map<String, boolean[]> marks) {}

    private static final Map<String, Draft> DRAFTS = new ConcurrentHashMap<>();

    public TakeAttendanceView(ApiClient api) {
        super(10);
        setPadding(new Insets(16));

        var title = new Label("Detalle de Materia · Toma de asistencia");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        var courseBox = new ComboBox<Course>();
        courseBox.setItems(FXCollections.observableArrayList(api.coursesForCurrentUser()));
        courseBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Course c) {
                return c == null ? "" : c.code() + " · " + c.name() + " · " + c.section();
            }

            @Override
            public Course fromString(String s) {
                return null;
            }
        });

        var header = new Label();
        header.setWrapText(true);
        header.setStyle("-fx-font-weight: bold;");

        var sessionList = new ListView<ClassSession>();
        sessionList.setPrefHeight(120);
        var detail = new VBox(8);
        var status = new Label();
        status.setWrapText(true);

        var btnExport = new Button("Exportar Listado a Excel (.xlsx)");
        btnExport.setOnAction(e -> {
            Course c = courseBox.getValue();
            if (c == null) {
                return;
            }
            var fc = new FileChooser();
            fc.setInitialFileName("asistencia-" + c.code() + ".xlsx");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
            var file = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
            if (file == null) {
                return;
            }
            try {
                List<ClassSession> sessions = api.validSessions(c.id());
                List<AttendanceExport.StudentRow> rows = new ArrayList<>();
                for (String student : STUDENTS) {
                    StudentSummary summary = api.studentSummary(c.id(), student);
                    rows.add(new AttendanceExport.StudentRow(
                            student, student, summary.attendedHours(), summary.missedHours(),
                            summary.percentage(), summary.risk().name()));
                }
                AttendanceExport.exportMatriz(c.code(), sessions, rows, file);
                status.setText("Exportado a " + file.getName() + " (US-14).");
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Export falló: " + ex.getMessage()).showAndWait();
            }
        });

        courseBox.getSelectionModel().selectedItemProperty().addListener((o, a, c) -> {
            if (c == null) {
                return;
            }
            header.setText(c.name() + " · " + c.code() + " · " + c.section()
                    + " · " + c.schedule() + " · Docente: " + c.teacher()
                    + " · Matriculados: " + STUDENTS.size());
            try {
                sessionList.setItems(FXCollections.observableArrayList(api.validSessions(c.id())));
                status.setText("Sesiones válidas: pasadas/hoy, sin Feriada/Suspendida ni futuras.");
            } catch (ApiException e) {
                status.setText("Error: " + e.getMessage());
            }
        });

        sessionList.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
            detail.getChildren().clear();
            if (s == null) {
                return;
            }
            var sessionHeader = new Label(
                    s.date() + " · " + s.blockHours() + "h · " + s.status()
                            + (s.status() == SessionStatus.PROGRAMADA ? " (pendiente)" : ""));
            sessionHeader.setStyle("-fx-font-weight: bold;");
            detail.getChildren().add(sessionHeader);

            Map<String, List<CheckBox>> boxes = new HashMap<>();
            Draft draft = DRAFTS.get(s.id());
            boolean hasDraft = draft != null && draft.date().equals(LocalDate.now());

            for (String student : STUDENTS) {
                var row = new HBox(8);
                StudentSummary summary = api.studentSummary(courseBox.getValue().id(), student);
                var info = new Label(String.format("%s · %.0fh · %.0f%% · %s",
                        student, summary.attendedHours(), summary.percentage(), summary.risk()));
                info.setMinWidth(220);
                row.getChildren().add(info);
                List<CheckBox> cbs = new ArrayList<>();
                boolean[] init = hasDraft ? draft.marks().get(student) : null;
                for (int i = 0; i < s.blockHours(); i++) {
                    var cb = new CheckBox("H" + (i + 1));
                    cb.setSelected(init != null && i < init.length ? init[i] : true);
                    cbs.add(cb);
                    row.getChildren().add(cb);
                }
                boxes.put(student, cbs);
                detail.getChildren().add(row);
            }
            if (hasDraft) {
                status.setText("Borrador recuperado de hoy (US-09).");
            }

            // US-09: draft autosave every 60 s on change (per-view Timeline)
            var dirty = new boolean[] {false};
            boxes.values().forEach(cbs -> cbs.forEach(cb ->
                    cb.selectedProperty().addListener((ob, ov, nv) -> dirty[0] = true)));
            Timeline autosave = new Timeline(new KeyFrame(javafx.util.Duration.seconds(60), ev -> {
                if (!dirty[0]) {
                    return;
                }
                Map<String, boolean[]> snap = snapshot(boxes);
                DRAFTS.put(s.id(), new Draft(LocalDate.now(), snap));
                dirty[0] = false;
                // Same-day editing only until 23:59 (next-day lock)
                status.setText("Borrador autoguardado "
                        + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                        + " (US-09, editable hasta 23:59).");
            }));
            autosave.setCycleCount(Timeline.INDEFINITE);
            autosave.play();

            var save = new Button("Guardar asistencia");
            save.setOnAction(e -> {
                // Next-day lock: teacher edits only current or past sessions
                if (s.date().isBefore(LocalDate.now().minusDays(60))) {
                    new Alert(Alert.AlertType.ERROR, "Sesión bloqueada para el docente.").showAndWait();
                    return;
                }
                Map<String, boolean[]> payload = snapshot(boxes);
                try {
                    api.saveAttendance(s.id(), payload);
                    DRAFTS.remove(s.id());
                    autosave.stop();
                    status.setText("Guardado " + LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                            + ". Sesión " + s.id() + " → DICTADA. % y margen recalculados.");
                    Course c = courseBox.getValue();
                    if (c != null) {
                        sessionList.setItems(FXCollections.observableArrayList(api.validSessions(c.id())));
                    }
                } catch (ApiException ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            });
            detail.getChildren().add(save);
        });

        if (!courseBox.getItems().isEmpty()) {
            courseBox.getSelectionModel().selectFirst();
        }
        getChildren().addAll(title, new Label("Materia (ciclo activo):"), courseBox, header,
                new Label("Fechas habilitadas:"), sessionList, detail, btnExport, status);
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

    // Tests only: draft access
    static Map<String, boolean[]> draftOf(String sessionId) {
        Draft d = DRAFTS.get(sessionId);
        return d == null ? null : Map.copyOf(d.marks());
    }

    static void saveDraft(String sessionId, Map<String, boolean[]> marks) {
        DRAFTS.put(sessionId, new Draft(LocalDate.now(), Map.copyOf(marks)));
    }
}

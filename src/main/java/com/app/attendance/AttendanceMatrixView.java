package com.app.attendance;

import com.app.api.ApiClient;
import com.app.api.dto.ClassSession;
import com.app.api.dto.ClassSession.SessionStatus;
import com.app.api.dto.Course;
import com.app.api.dto.StudentSummary;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * US-16: students × dates matrix with colors, tooltip, and pending marks.
 * Per-cell detail: DICTADA (block hours) vs PROGRAMADA
 * (pending).
 */
public class AttendanceMatrixView extends VBox {

    private static final List<String> STUDENTS = List.of("alu-01", "alu-02", "alu-03");

    public AttendanceMatrixView(ApiClient api) {
        super(10);
        setPadding(new Insets(16));
        var title = new Label("Histórico · matriz de asistencia");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        var courseBox = new ComboBox<Course>();
        courseBox.setItems(FXCollections.observableArrayList(api.coursesForCurrentUser()));
        courseBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Course c) {
                return c == null ? "" : c.code();
            }

            @Override
            public Course fromString(String s) {
                return null;
            }
        });
        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        var info = new Label();
        info.setWrapText(true);

        courseBox.getSelectionModel().selectedItemProperty().addListener((o, a, c) -> {
            grid.getChildren().clear();
            if (c == null) {
                return;
            }
            List<ClassSession> sessions = api.validSessions(c.id());
            grid.add(new Label("Alumno \\ Fecha"), 0, 0);
            for (int col = 0; col < sessions.size(); col++) {
                grid.add(new Label(sessions.get(col).date().toString()), col + 1, 0);
            }
            for (int row = 0; row < STUDENTS.size(); row++) {
                String student = STUDENTS.get(row);
                grid.add(new Label(student), 0, row + 1);
                StudentSummary summary = api.studentSummary(c.id(), student);
                for (int col = 0; col < sessions.size(); col++) {
                    ClassSession s = sessions.get(col);
                    var cell = new Label();
                    cell.setMinWidth(64);
                    if (s.status() == SessionStatus.DICTADA) {
                        cell.setText("● " + s.blockHours() + "h");
                        cell.setStyle("-fx-background-color: #1f6feb; -fx-text-fill: white; -fx-padding: 4;");
                        cell.setTooltip(new Tooltip(
                                student + " · " + s.date() + " · Dictada · resumen: "
                                        + String.format("%.1f%%", summary.percentage())));
                    } else {
                        cell.setText("○ pend.");
                        cell.setStyle("-fx-background-color: #6e7681; -fx-text-fill: white; -fx-padding: 4;");
                        cell.setTooltip(new Tooltip(student + " · " + s.date() + " · Pendiente de registro"));
                    }
                    grid.add(cell, col + 1, row + 1);
                }
            }
            info.setText("Filas=alumnos, columnas=fechas. ● Dictada · ○ pendiente. Filtros: ciclo/materia (MVP: materia).");
        });
        if (!courseBox.getItems().isEmpty()) {
            courseBox.getSelectionModel().selectFirst();
        }
        getChildren().addAll(title, new Label("Materia:"), courseBox, grid, info);
    }
}

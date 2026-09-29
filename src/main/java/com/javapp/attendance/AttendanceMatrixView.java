package com.javapp.attendance;

import com.javapp.api.ApiClient;
import com.javapp.api.dto.Materia;
import com.javapp.api.dto.SesionClase;
import com.javapp.api.dto.StudentSummary;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * US-16 (MVP): matriz alumnos × fechas con colores, tooltip y pendientes.
 * Detalle por celda: DICTADA (horas del bloque) vs PROGRAMADA (pendiente).
 */
public class AttendanceMatrixView extends VBox {

    private static final List<String> ALUMNOS = List.of("alu-01", "alu-02", "alu-03");

    public AttendanceMatrixView(ApiClient api) {
        super(10);
        setPadding(new Insets(16));
        var title = new Label("Histórico · matriz de asistencia");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        var materiaBox = new ComboBox<Materia>();
        materiaBox.setItems(FXCollections.observableArrayList(api.materiasForCurrentUser()));
        materiaBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Materia m) {
                return m == null ? "" : m.codigo();
            }

            @Override
            public Materia fromString(String s) {
                return null;
            }
        });
        var grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(6);
        var info = new Label();
        info.setWrapText(true);

        materiaBox.getSelectionModel().selectedItemProperty().addListener((o, a, m) -> {
            grid.getChildren().clear();
            if (m == null) {
                return;
            }
            List<SesionClase> sesiones = api.sesionesValidas(m.id());
            grid.add(new Label("Alumno \\ Fecha"), 0, 0);
            for (int c = 0; c < sesiones.size(); c++) {
                grid.add(new Label(sesiones.get(c).fecha().toString()), c + 1, 0);
            }
            for (int r = 0; r < ALUMNOS.size(); r++) {
                String alu = ALUMNOS.get(r);
                grid.add(new Label(alu), 0, r + 1);
                StudentSummary sum = api.resumenAlumno(m.id(), alu);
                for (int c = 0; c < sesiones.size(); c++) {
                    SesionClase s = sesiones.get(c);
                    var cell = new Label();
                    cell.setMinWidth(64);
                    if (s.estado() == SesionClase.EstadoSesion.DICTADA) {
                        cell.setText("● " + s.bloqueHoras() + "h");
                        cell.setStyle("-fx-background-color: #1f6feb; -fx-text-fill: white; -fx-padding: 4;");
                        cell.setTooltip(new Tooltip(
                                alu + " · " + s.fecha() + " · Dictada · resumen: "
                                        + String.format("%.1f%%", sum.porcentaje())));
                    } else {
                        cell.setText("○ pend.");
                        cell.setStyle("-fx-background-color: #6e7681; -fx-text-fill: white; -fx-padding: 4;");
                        cell.setTooltip(new Tooltip(alu + " · " + s.fecha() + " · Pendiente de registro"));
                    }
                    grid.add(cell, c + 1, r + 1);
                }
            }
            info.setText("Filas=alumnos, columnas=fechas. ● Dictada · ○ pendiente. Filtros: ciclo/materia (MVP: materia).");
        });
        if (!materiaBox.getItems().isEmpty()) {
            materiaBox.getSelectionModel().selectFirst();
        }
        getChildren().addAll(title, new Label("Materia:"), materiaBox, grid, info);
    }
}

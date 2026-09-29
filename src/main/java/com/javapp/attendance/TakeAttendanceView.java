package com.javapp.attendance;

import com.javapp.api.ApiClient;
import com.javapp.api.ApiException;
import com.javapp.api.dto.Materia;
import com.javapp.api.dto.SesionClase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/**
 * US-08: solo fechas válidas, N checkboxes por hora (marcado=Presente),
 * guardar → DICTADA + recálculo.
 */
public class TakeAttendanceView extends VBox {

    private static final List<String> ALUMNOS = List.of("alu-01", "alu-02", "alu-03");

    public TakeAttendanceView(ApiClient api) {
        super(10);
        setPadding(new Insets(16));

        var title = new Label("Tomar asistencia");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        var materiaBox = new ComboBox<Materia>();
        materiaBox.setItems(FXCollections.observableArrayList(api.materiasForCurrentUser()));
        materiaBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Materia m) {
                return m == null ? "" : m.codigo() + " · " + m.nombre();
            }

            @Override
            public Materia fromString(String s) {
                return null;
            }
        });

        var sesionesList = new ListView<SesionClase>();
        sesionesList.setPrefHeight(140);
        var detail = new VBox(8);
        var status = new Label();
        status.setWrapText(true);

        materiaBox.getSelectionModel().selectedItemProperty().addListener((o, a, m) -> {
            if (m == null) {
                return;
            }
            try {
                sesionesList.setItems(FXCollections.observableArrayList(api.sesionesValidas(m.id())));
                status.setText("Sesiones válidas: pasadas/hoy, sin Feriada/Suspendida ni futuras.");
            } catch (ApiException e) {
                status.setText("Error: " + e.getMessage());
            }
        });

        sesionesList.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> {
            detail.getChildren().clear();
            if (s == null) {
                return;
            }
            var header = new Label(
                    s.fecha() + " · " + s.bloqueHoras() + "h · " + s.estado()
                            + (s.estado() == SesionClase.EstadoSesion.PROGRAMADA ? " (pendiente)" : ""));
            header.setStyle("-fx-font-weight: bold;");
            detail.getChildren().add(header);
            Map<String, List<CheckBox>> boxes = new HashMap<>();
            for (String alu : ALUMNOS) {
                var row = new HBox(8);
                row.getChildren().add(new Label(alu));
                List<CheckBox> cbs = new ArrayList<>();
                for (int i = 0; i < s.bloqueHoras(); i++) {
                    var cb = new CheckBox("H" + (i + 1));
                    cb.setSelected(true);
                    cbs.add(cb);
                    row.getChildren().add(cb);
                }
                boxes.put(alu, cbs);
                detail.getChildren().add(row);
            }
            var save = new Button("Guardar asistencia");
            save.setOnAction(e -> {
                Map<String, boolean[]> payload = new HashMap<>();
                boxes.forEach((alu, cbs) -> {
                    boolean[] arr = new boolean[cbs.size()];
                    for (int i = 0; i < cbs.size(); i++) {
                        arr[i] = cbs.get(i).isSelected();
                    }
                    payload.put(alu, arr);
                });
                try {
                    api.guardarAsistencia(s.id(), payload);
                    status.setText("Guardado. Sesión " + s.id() + " → DICTADA. % y margen recalculados.");
                    // refrescar lista (PROGRAMADA → DICTADA)
                    Materia m = materiaBox.getValue();
                    if (m != null) {
                        sesionesList.setItems(FXCollections.observableArrayList(api.sesionesValidas(m.id())));
                    }
                } catch (ApiException ex) {
                    new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                }
            });
            detail.getChildren().add(save);
        });

        if (!materiaBox.getItems().isEmpty()) {
            materiaBox.getSelectionModel().selectFirst();
        }
        getChildren().addAll(title, new Label("Materia:"), materiaBox,
                new Label("Fechas habilitadas:"), sesionesList, detail, status);
    }
}

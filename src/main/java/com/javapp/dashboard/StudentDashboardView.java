package com.javapp.dashboard;

import com.javapp.api.ApiClient;
import com.javapp.api.dto.Materia;
import com.javapp.api.dto.StudentSummary;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/** US-15: materias, % asistencia, margen de faltas con semáforo BR-06. */
public class StudentDashboardView extends VBox {

    public StudentDashboardView(ApiClient api, String alumnoId) {
        super(10);
        setPadding(new Insets(16));
        var title = new Label("Mi asistencia · " + alumnoId);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        getChildren().add(title);
        for (Materia m : api.materiasForCurrentUser()) {
            StudentSummary r = api.resumenAlumno(m.id(), alumnoId);
            var card = new VBox(4);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-border-color: gray; -fx-border-radius: 8;");
            var name = new Label(m.codigo() + " · " + m.nombre());
            name.setStyle("-fx-font-weight: bold;");
            var pct = new Label(String.format("Asistencia: %.1f%%  (%.0f/%.0f h)",
                    r.porcentaje(), r.hAsistidas(), r.hDictadas()));
            var margen = new Label(String.format("Margen: %.0f h restantes de %.0f h · %s",
                    r.restantesHoras(), r.margenHoras(), r.riesgo()));
            Color c = switch (r.riesgo()) {
                case VERDE -> Color.GREEN;
                case AMARILLO -> Color.ORANGE;
                case ROJO -> Color.RED;
            };
            margen.setTextFill(c);
            var note = new Label(r.riesgo() == com.javapp.common.RiskLevel.ROJO
                    ? "En DPI: superaste el margen de faltas."
                    : "Justificadas y Exento no descuentan del margen.");
            note.setWrapText(true);
            card.getChildren().addAll(name, pct, margen, note);
            getChildren().add(card);
        }
    }
}

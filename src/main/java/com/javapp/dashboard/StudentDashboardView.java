package com.javapp.dashboard;

import com.javapp.api.ApiClient;
import com.javapp.api.dto.Course;
import com.javapp.api.dto.StudentSummary;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/** US-15 + mvp.md §3: cards with section/teacher/schedule, hour breakdown, BR-03 %, BR-06 semaphore. */
public class StudentDashboardView extends VBox {

    public StudentDashboardView(ApiClient api, String studentId) {
        super(10);
        setPadding(new Insets(16));
        var title = new Label("Lista de Materias · Ciclo Activo — " + studentId);
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        getChildren().add(title);
        for (Course c : api.coursesForCurrentUser()) {
            StudentSummary summary = api.studentSummary(c.id(), studentId);
            var card = new VBox(4);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-border-color: gray; -fx-border-radius: 8;");
            var name = new Label(c.code() + " · " + c.name() + " · " + c.section());
            name.setStyle("-fx-font-weight: bold;");
            var ctx = new Label("Docente: " + c.teacher() + "  ·  Horario: " + c.schedule());
            ctx.setWrapText(true);
            var breakdown = new Label(String.format(
                    "Dictadas: %.0f h · Asistidas: %.0f h · Justificadas: %.0f h · Exentas: %.0f h",
                    summary.taughtHours(), summary.attendedHours(), summary.excusedHours(), summary.exemptHours()));
            breakdown.setWrapText(true);
            var pct = new Label(String.format("Asistencia a la fecha (BR-03): %.1f%%",
                    summary.percentage()));
            pct.setStyle("-fx-font-weight: bold;");
            var margin = new Label(String.format("Margen faltas (BR-06): %.0f h restantes de %.0f h · %s",
                    summary.remainingHours(), summary.marginHours(), summary.risk()));
            Color color = switch (summary.risk()) {
                case VERDE -> Color.GREEN;
                case AMARILLO -> Color.ORANGE;
                case ROJO -> Color.RED;
            };
            margin.setTextFill(color);
            var note = new Label(summary.risk() == com.javapp.common.RiskLevel.ROJO
                    ? "Rojo: condición DPI aplicable (consumo ≥100% del margen)."
                    : "Justificada y Exento no descuentan del margen (BR-02/BR-04).");
            note.setWrapText(true);
            card.getChildren().addAll(name, ctx, breakdown, pct, margin, note);
            getChildren().add(card);
        }
    }
}

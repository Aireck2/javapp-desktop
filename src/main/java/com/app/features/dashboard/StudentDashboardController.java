package com.app.features.dashboard;

import com.app.common.RiskLevel;
import com.app.models.StudentCourseModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Renders dashboard screen states from its view model. */
public final class StudentDashboardController {

  @FXML private Label studentTitle;
  @FXML private StackPane stateContainer;

  private final String studentId;
  private final StudentDashboardViewModel viewModel;

  public StudentDashboardController(String studentId, StudentDashboardViewModel viewModel) {
    this.studentId = studentId;
    this.viewModel = viewModel;
  }

  @FXML
  private void initialize() {
    studentTitle.setText("Lista de Materias · Ciclo Activo — " + studentId);
    viewModel.load(
        state -> {
          if (Platform.isFxApplicationThread()) {
            render(state);
          } else {
            Platform.runLater(() -> render(state));
          }
        });
  }

  private void render(StudentDashboardViewModel.ScreenState state) {
    switch (state.status()) {
      case LOADING -> {
        VBox loading = new VBox(10, new ProgressIndicator(), new Label("Cargando resumen…"));
        loading.getStyleClass().add("course-screen-state");
        loading.setAlignment(javafx.geometry.Pos.CENTER);
        stateContainer.getChildren().setAll(loading);
      }
      case EMPTY -> stateContainer.getChildren().setAll(message(state.message(), false));
      case ERROR -> stateContainer.getChildren().setAll(message(state.message(), true));
      case CONTENT -> {
        VBox cards = new VBox(12);
        cards.getStyleClass().add("dashboard-course-list");
        state.courses().forEach(course -> cards.getChildren().add(courseCard(course)));
        stateContainer.getChildren().setAll(cards);
      }
    }
  }

  private static VBox courseCard(StudentCourseModel course) {
    VBox card = new VBox(5);
    card.getStyleClass().add("dashboard-course-card");
    Label name = new Label(course.code() + " · " + course.name() + " · " + course.section());
    name.getStyleClass().add("dashboard-course-name");
    Label context =
        new Label("Docente: " + course.teacher() + "  ·  Horario: " + course.schedule());
    context.getStyleClass().add("dashboard-course-context");
    context.setWrapText(true);
    Label breakdown =
        new Label(
            String.format(
                "Dictadas: %.0f h · Asistidas: %.0f h · Justificadas: %.0f h · Exentas: %.0f h",
                course.taughtHours(),
                course.attendedHours(),
                course.excusedHours(),
                course.exemptHours()));
    breakdown.getStyleClass().add("dashboard-course-context");
    breakdown.setWrapText(true);
    Label percentage =
        new Label(
            String.format("Asistencia a la fecha (BR-03): %.1f%%", course.attendancePercentage()));
    percentage.getStyleClass().add("dashboard-course-name");
    Label margin =
        new Label(
            String.format(
                "Margen faltas (BR-06): %.0f h restantes de %.0f h · %s",
                course.remainingHours(), course.marginHours(), course.risk()));
    margin
        .getStyleClass()
        .add(
            switch (course.risk()) {
              case VERDE -> "risk-green";
              case AMARILLO -> "risk-amber";
              case ROJO -> "risk-red";
            });
    Label note =
        new Label(
            course.risk() == RiskLevel.ROJO
                ? "Rojo: condición DPI aplicable (consumo ≥100% del margen)."
                : "Justificada y Exento no descuentan del margen (BR-02/BR-04).");
    note.getStyleClass().add("dashboard-course-context");
    note.setWrapText(true);
    card.getChildren().addAll(name, context, breakdown, percentage, margin, note);
    return card;
  }

  private static VBox message(String text, boolean error) {
    Label label = new Label(text);
    label.setWrapText(true);
    VBox box = new VBox(label);
    box.setAlignment(javafx.geometry.Pos.CENTER);
    box.getStyleClass().add("course-screen-state");
    if (error) {
      box.getStyleClass().add("error-state");
    }
    return box;
  }
}

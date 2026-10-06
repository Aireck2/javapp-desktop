package com.app.features.dashboard;

import com.app.common.RiskLevel;
import com.app.models.StudentCourseModel;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Renders dashboard screen states from its view model. */
public final class StudentDashboardController {

  @FXML private StackPane stateContainer;

  private final StudentDashboardViewModel viewModel;

  public StudentDashboardController(StudentDashboardViewModel viewModel) {
    this.viewModel = viewModel;
  }

  @FXML
  private void initialize() {
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
        VBox cards = new VBox(16);
        cards.getStyleClass().add("dashboard-course-list");
        state.courses().forEach(course -> cards.getChildren().add(courseCard(course)));
        stateContainer.getChildren().setAll(cards);
      }
    }
  }

  private static VBox courseCard(StudentCourseModel course) {
    VBox card = new VBox(16);
    card.getStyleClass().add("dashboard-course-card");
    card.setMaxWidth(Double.MAX_VALUE);
    card.getChildren()
        .addAll(courseHeader(course), attendanceSection(course), alert(course), stats(course));
    return card;
  }

  private static HBox courseHeader(StudentCourseModel course) {
    VBox identity = new VBox(3);
    Label code = new Label(course.code());
    code.getStyleClass().add("student-course-code");
    Label name = new Label(course.name());
    name.setWrapText(true);
    name.getStyleClass().add("student-course-title");
    Label teacher = new Label(course.teacher());
    teacher.setWrapText(true);
    teacher.getStyleClass().add("student-course-teacher");
    identity.getChildren().addAll(code, name, teacher);
    HBox.setHgrow(identity, Priority.ALWAYS);

    Label badge = new Label(statusText(course.risk()));
    badge.getStyleClass().add("student-course-status");
    badge.getStyleClass().add(statusClass(course.risk()));
    badge.setWrapText(true);

    HBox header = new HBox(12, identity, badge);
    header.setAlignment(Pos.CENTER_LEFT);
    return header;
  }

  private static VBox attendanceSection(StudentCourseModel course) {
    double percentage = Math.max(0, Math.min(100, course.attendancePercentage()));
    Label caption = new Label("ASISTENCIA EFECTIVA (BR-03)");
    caption.getStyleClass().add("student-course-caption");
    Label value = new Label(format(percentage) + "%");
    value.getStyleClass().add("student-course-percentage");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox heading = new HBox(caption, spacer, value);
    heading.setAlignment(Pos.BOTTOM_LEFT);

    ProgressBar progress = new ProgressBar(percentage / 100.0);
    progress.setMaxWidth(Double.MAX_VALUE);
    progress.getStyleClass().add("student-course-progress");

    Label zero = scaleLabel("0%");
    Label limit = scaleLabel("Límite DPI (70% asistencia)");
    Region scaleSpacer = new Region();
    HBox.setHgrow(scaleSpacer, Priority.ALWAYS);
    Region endSpacer = new Region();
    HBox.setHgrow(endSpacer, Priority.ALWAYS);
    Label hundred = scaleLabel("100%");
    HBox scale = new HBox(zero, scaleSpacer, limit, endSpacer, hundred);
    scale.setAlignment(Pos.CENTER_LEFT);
    return new VBox(5, heading, progress, scale);
  }

  private static VBox alert(StudentCourseModel course) {
    boolean attention = course.risk() != RiskLevel.VERDE;
    VBox box = new VBox(4);
    box.getStyleClass().add(attention ? "student-course-alert-warning" : "student-course-alert-ok");
    Label title =
        new Label(
            "Horas de falta consumidas: "
                + format(course.missedHours())
                + " h de "
                + format(course.marginHours())
                + " h permitidas");
    title.setWrapText(true);
    title.getStyleClass().add("student-course-alert-title");
    Label detail =
        new Label(
            "Le quedan "
                + format(course.remainingHours())
                + " horas de margen"
                + (course.risk() == RiskLevel.ROJO
                    ? ". Se alcanzó el límite DPI."
                    : attention ? " antes de alcanzar el límite DPI." : ". Margen saludable."));
    detail.setWrapText(true);
    detail.getStyleClass().add("student-course-alert-detail");
    box.getChildren().addAll(title, detail);
    return box;
  }

  private static HBox stats(StudentCourseModel course) {
    HBox row = new HBox(10);
    row.setAlignment(Pos.CENTER);
    row.getStyleClass().add("student-course-stats");
    row.getChildren()
        .addAll(
            stat("Asistidas", course.attendedHours(), false),
            stat("Faltas", course.missedHours(), course.missedHours() > 0),
            stat("Justificadas", course.excusedHours(), false),
            stat("Exentas", course.exemptHours(), false));
    return row;
  }

  private static VBox stat(String title, double hours, boolean warning) {
    VBox box = new VBox(3);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(8));
    HBox.setHgrow(box, Priority.ALWAYS);
    box.getStyleClass().add("student-course-stat");
    Label heading = new Label(title);
    heading
        .getStyleClass()
        .add(warning ? "student-course-stat-title-warning" : "student-course-stat-title");
    Label value = new Label(format(hours) + " h");
    value
        .getStyleClass()
        .add(warning ? "student-course-stat-value-warning" : "student-course-stat-value");
    box.getChildren().addAll(heading, value);
    return box;
  }

  private static Label scaleLabel(String text) {
    Label label = new Label(text);
    label.getStyleClass().add("student-course-scale-label");
    return label;
  }

  private static String statusText(RiskLevel risk) {
    return switch (risk) {
      case VERDE -> "● Estado Normal";
      case AMARILLO -> "● Atención · Margen ajustado";
      case ROJO -> "● Atención · Límite DPI";
    };
  }

  private static String statusClass(RiskLevel risk) {
    return switch (risk) {
      case VERDE -> "student-course-status-ok";
      case AMARILLO, ROJO -> "student-course-status-warning";
    };
  }

  private static String format(double value) {
    DecimalFormat format =
        new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-PE")));
    return format.format(value);
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

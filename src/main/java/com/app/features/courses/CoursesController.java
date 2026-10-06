package com.app.features.courses;

import com.app.api.dto.ClassSession;
import com.app.components.CourseCard;
import com.app.models.CourseModel;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Connects course screen events and visual states to the courses view model. */
public final class CoursesController {

  @FXML private StackPane stateContainer;

  private final CoursesViewModel viewModel;
  private final Consumer<ClassSession> onDetail;
  private final Consumer<ClassSession> onTakeAttendance;

  public CoursesController(
      CoursesViewModel viewModel,
      Consumer<ClassSession> onDetail,
      Consumer<ClassSession> onTakeAttendance) {
    this.viewModel = viewModel;
    this.onDetail = onDetail;
    this.onTakeAttendance = onTakeAttendance;
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

  private void render(CoursesViewModel.ScreenState state) {
    switch (state.status()) {
      case LOADING -> {
        VBox loading = centeredState();
        loading.getChildren().addAll(new ProgressIndicator(), new Label("Cargando cursos…"));
        stateContainer.getChildren().setAll(loading);
      }
      case EMPTY -> stateContainer.getChildren().setAll(message(state.message(), "empty-state"));
      case ERROR -> stateContainer.getChildren().setAll(message(state.message(), "error-state"));
      case CONTENT -> stateContainer.getChildren().setAll(buildTimeline(state.timeline()));
    }
  }

  private VBox buildTimeline(CoursesService.Timeline timeline) {
    VBox days = new VBox(18);
    days.getStyleClass().add("course-timeline");
    for (CoursesService.CourseDay day : timeline.days()) {
      VBox section = new VBox(10);
      section.getStyleClass().add("course-day-section");
      Label heading = new Label(day.title());
      heading.getStyleClass().add(day.today() ? "course-day-title-today" : "course-day-title");
      section.getChildren().add(heading);

      if (day.sessions().isEmpty()) {
        Label noSession =
            new Label(day.today() ? "Sin sesión programada para hoy." : "Sin sesiones este día.");
        noSession.getStyleClass().add("course-day-empty");
        section.getChildren().add(noSession);
      } else {
        for (CoursesService.SessionCourse entry : day.sessions()) {
          CourseModel model = entry.course();
          CourseCard card =
              new CourseCard(
                  model,
                  true,
                  selected -> {
                    ClassSession session = entry.session();
                    if ("Clase Hoy".equalsIgnoreCase(selected.badgeType())
                        && onTakeAttendance != null) {
                      onTakeAttendance.accept(session);
                    } else if (onDetail != null) {
                      onDetail.accept(session);
                    }
                  });
          section.getChildren().add(card);
        }
      }
      days.getChildren().add(section);
    }
    return days;
  }

  private static VBox message(String text, String styleClass) {
    VBox box = centeredState();
    box.getStyleClass().add(styleClass);
    Label label = new Label(text);
    label.setWrapText(true);
    box.getChildren().add(label);
    return box;
  }

  private static VBox centeredState() {
    VBox box = new VBox(10);
    box.setAlignment(Pos.CENTER);
    box.getStyleClass().add("course-screen-state");
    return box;
  }
}

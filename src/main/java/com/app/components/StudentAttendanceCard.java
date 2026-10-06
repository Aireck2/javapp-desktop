package com.app.components;

import com.app.models.StudentAttendanceModel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Reusable student card with the session marks and course attendance metrics. */
public final class StudentAttendanceCard extends VBox {

  public StudentAttendanceCard(
      StudentAttendanceModel student, boolean editable, Runnable onMarksChanged) {
    setSpacing(10);
    setPadding(new Insets(14));
    getStyleClass().add("student-attendance-card");

    HBox topRow = new HBox(8);
    topRow.setAlignment(Pos.CENTER_LEFT);

    Label index = new Label(student.index());
    index.getStyleClass().add("student-card-index");

    Label name = new Label(student.name());
    name.setWrapText(true);
    name.getStyleClass().add("student-card-name");
    Label code = new Label("Cód. " + student.code());
    code.getStyleClass().add("student-card-code");
    VBox identity = new VBox(2, name, code);
    HBox.setHgrow(identity, Priority.ALWAYS);

    Label badge = createBadge(currentStatus(student));
    topRow.getChildren().addAll(index, identity, badge);

    VBox content = new VBox(10);
    if (student.note() != null && !student.note().isBlank()) {
      Label note = new Label(student.note());
      note.setWrapText(true);
      note.setPadding(new Insets(8, 12, 8, 12));
      note.setMaxWidth(Double.MAX_VALUE);
      note.getStyleClass().add("student-card-note");
      content.getChildren().add(note);
    } else {
      HBox blocks = new HBox(6);
      blocks.setAlignment(Pos.CENTER);
      for (StudentAttendanceModel.HourBlock block : student.blocks()) {
        CheckBox mark = new CheckBox(block.time());
        mark.setSelected(block.isChecked());
        mark.setDisable(!editable);
        mark.setMaxWidth(Double.MAX_VALUE);
        mark.setAlignment(Pos.CENTER);
        mark.getStyleClass().add("student-hour-mark");
        mark.getStyleClass()
            .add(block.isChecked() ? "student-hour-mark-checked" : "student-hour-mark-unchecked");
        mark.selectedProperty()
            .addListener(
                (observable, oldValue, selected) -> {
                  block.setChecked(selected);
                  updateBadge(badge, student);
                  mark.getStyleClass()
                      .removeAll("student-hour-mark-checked", "student-hour-mark-unchecked");
                  mark.getStyleClass()
                      .add(selected ? "student-hour-mark-checked" : "student-hour-mark-unchecked");
                  if (onMarksChanged != null) {
                    onMarksChanged.run();
                  }
                });
        HBox.setHgrow(mark, Priority.ALWAYS);
        blocks.getChildren().add(mark);
      }
      content.getChildren().add(blocks);
    }

    GridPane metrics = createMetrics(student);
    getChildren().addAll(topRow, content, metrics);
  }

  private static GridPane createMetrics(StudentAttendanceModel student) {
    GridPane grid = new GridPane();
    grid.setHgap(4);
    grid.setAlignment(Pos.CENTER);
    grid.getStyleClass().add("student-card-metrics");
    for (int i = 0; i < 5; i++) {
      ColumnConstraints column = new ColumnConstraints();
      column.setPercentWidth(20);
      column.setHalignment(javafx.geometry.HPos.CENTER);
      grid.getColumnConstraints().add(column);
    }
    String[] headings = {"Prog.", "Pres.", "Aus.", "% Asis.", "% Inas."};
    String[] values = {
      String.valueOf(student.prog()),
      student.pres() == 0 && student.aus() == 0 ? "--" : String.valueOf(student.pres()),
      student.pres() == 0 && student.aus() == 0 ? "--" : String.valueOf(student.aus()),
      student.isExempt() ? "Exento" : student.pctAsis() + "%",
      student.isExempt() ? "0%" : student.pctInas() + "%"
    };
    for (int i = 0; i < headings.length; i++) {
      Label heading = new Label(headings[i]);
      heading.getStyleClass().add("student-metric-heading");
      Label value = new Label(values[i]);
      value.getStyleClass().add("student-metric-value");
      boolean negative =
          (i == 2 && student.aus() > 0)
              || (i == 3 && student.pctAsis() < 80)
              || (i == 4 && student.pctInas() > 15);
      value
          .getStyleClass()
          .add(i == 0 ? "metric-muted" : negative ? "metric-negative" : "metric-normal");
      grid.add(heading, i, 0);
      grid.add(value, i, 1);
    }
    return grid;
  }

  private static Label createBadge(String text) {
    Label badge = new Label(text);
    applyBadgeStyle(badge, text);
    return badge;
  }

  private static void updateBadge(Label badge, StudentAttendanceModel student) {
    applyBadgeStyle(badge, currentStatus(student));
  }

  private static String currentStatus(StudentAttendanceModel student) {
    if (student.isExempt()) return "Exento RR-04";
    long present =
        student.blocks().stream().filter(StudentAttendanceModel.HourBlock::isChecked).count();
    int total = student.blocks().size();
    return present == total
        ? "Presente Completo"
        : present == 0 ? "Falta Total" : "Parcial (" + present + "h / " + total + "h)";
  }

  private static void applyBadgeStyle(Label badge, String text) {
    badge.setText(text);
    badge
        .getStyleClass()
        .removeAll(
            "student-status-present",
            "student-status-exempt",
            "student-status-absent",
            "student-status-partial");
    badge.getStyleClass().add("student-status-badge");
    badge
        .getStyleClass()
        .add(
            switch (text) {
              case "Presente Completo" -> "student-status-present";
              case "Exento RR-04" -> "student-status-exempt";
              case "Falta Total" -> "student-status-absent";
              default -> "student-status-partial";
            });
  }
}

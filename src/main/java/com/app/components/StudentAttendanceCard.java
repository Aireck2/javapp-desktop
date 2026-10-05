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

    public StudentAttendanceCard(StudentAttendanceModel student, boolean editable, Runnable onMarksChanged) {
        setSpacing(10);
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; "
                + "-fx-border-color: #E2E8F0; -fx-border-radius: 12px; -fx-border-width: 1px;");

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label index = new Label(student.index());
        index.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; "
                + "-fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 3 7 3 7; "
                + "-fx-background-radius: 6px;");

        Label name = new Label(student.name());
        name.setWrapText(true);
        name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        Label code = new Label("Cód. " + student.code());
        code.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
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
            note.setStyle("-fx-background-color: #EFF6FF; -fx-background-radius: 8px; "
                    + "-fx-text-fill: #1E40AF; -fx-font-size: 11px;");
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
                mark.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; "
                        + "-fx-background-color: " + (block.isChecked() ? "#DCFCE7" : "#F1F5F9")
                        + "; -fx-background-radius: 8px; -fx-padding: 8 4 8 4;");
                mark.selectedProperty().addListener((observable, oldValue, selected) -> {
                    block.setChecked(selected);
                    updateBadge(badge, student);
                    mark.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; "
                            + "-fx-background-color: " + (selected ? "#DCFCE7" : "#F1F5F9")
                            + "; -fx-background-radius: 8px; -fx-padding: 8 4 8 4;");
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
        grid.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8px; -fx-padding: 8;");
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
        String[] colors = {
                "#64748B", "#0F172A", student.aus() > 0 ? "#DC2626" : "#0F172A",
                student.pctAsis() < 80 ? "#DC2626" : "#0F172A",
                student.pctInas() > 15 ? "#DC2626" : "#0F172A"
        };
        for (int i = 0; i < headings.length; i++) {
            Label heading = new Label(headings[i]);
            heading.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
            Label value = new Label(values[i]);
            value.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + colors[i] + ";");
            grid.add(heading, i, 0);
            grid.add(value, i, 1);
        }
        return grid;
    }

    private static Label createBadge(String text) {
        Label badge = new Label(text);
        badge.setPadding(new Insets(3, 8, 3, 8));
        applyBadgeStyle(badge, text);
        return badge;
    }

    private static void updateBadge(Label badge, StudentAttendanceModel student) {
        applyBadgeStyle(badge, currentStatus(student));
    }

    private static String currentStatus(StudentAttendanceModel student) {
        if (student.isExempt()) return "Exento RR-04";
        long present = student.blocks().stream().filter(StudentAttendanceModel.HourBlock::isChecked).count();
        int total = student.blocks().size();
        return present == total ? "Presente Completo"
                : present == 0 ? "Falta Total" : "Parcial (" + present + "h / " + total + "h)";
    }

    private static void applyBadgeStyle(Label badge, String text) {
        badge.setText(text);
        String color = switch (text) {
            case "Presente Completo" -> "-fx-background-color: #DCFCE7; -fx-text-fill: #166534;";
            case "Exento RR-04" -> "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF;";
            case "Falta Total" -> "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;";
            default -> "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;";
        };
        badge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 12px; " + color);
    }
}

package com.app.components;

import com.app.models.CourseModel;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Tarjeta de curso reutilizable adaptada visual y funcionalmente según el rol (Docente / Alumno).
 */
public class CourseCard extends VBox {

    private final CourseModel course;
    private final boolean isTeacherRole;

    public CourseCard(CourseModel course, boolean isTeacherRole) {
        this(course, isTeacherRole, null);
    }

    public CourseCard(CourseModel course, boolean isTeacherRole, Consumer<CourseModel> onAction) {
        this.course = course;
        this.isTeacherRole = isTeacherRole;

        setSpacing(12);
        setPadding(new Insets(16));
        setStyle(
            "-fx-background-color: #FFFFFF; " +
            "-fx-background-radius: 14px; " +
            "-fx-border-color: #E2E8F0; " +
            "-fx-border-radius: 14px; " +
            "-fx-border-width: 1px;"
        );

        // Header de la tarjeta: Código/Sección + Badge
        Label codeLabel = new Label(course.code() + " • SECCIÓN " + course.section());
        codeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-font-weight: bold;");

        Label badge = new Label(course.badgeType());
        applyBadgeStyle(badge, course.badgeType());

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topRow = new HBox(codeLabel, topSpacer, badge);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Título del curso
        Label courseName = new Label(course.name());
        courseName.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
        courseName.setWrapText(true);

        // Info de horario y aula con íconos
        HBox infoRow = new HBox(16);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        FontIcon clockIcon = new FontIcon(Feather.CLOCK);
        clockIcon.setIconSize(13);
        clockIcon.setIconColor(Color.web("#64748B"));
        Label scheduleLabel = new Label(course.schedule());
        scheduleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        HBox scheduleBox = new HBox(5, clockIcon, scheduleLabel);
        scheduleBox.setAlignment(Pos.CENTER_LEFT);

        FontIcon roomIcon = new FontIcon(Feather.MAP_PIN);
        roomIcon.setIconSize(13);
        roomIcon.setIconColor(Color.web("#64748B"));
        Label roomLabel = new Label(course.classroom());
        roomLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        HBox roomBox = new HBox(5, roomIcon, roomLabel);
        roomBox.setAlignment(Pos.CENTER_LEFT);

        infoRow.getChildren().addAll(scheduleBox, roomBox);

        // Barra de avance
        String studentsText = course.enrolledStudents() + (isTeacherRole ? " alumnos" : " compañeros");
        Label studentsLabel = new Label(studentsText);
        studentsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        String hoursText = course.completedHours() + "/" + course.totalHours() + " h (" + course.getProgressPercentage() + "%)";
        Label hoursLabel = new Label(hoursText);
        hoursLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        Region progressSpacer = new Region();
        HBox.setHgrow(progressSpacer, Priority.ALWAYS);
        HBox progressTextRow = new HBox(studentsLabel, progressSpacer, hoursLabel);
        progressTextRow.setAlignment(Pos.CENTER_LEFT);

        double progressRatio = course.totalHours() > 0 ? (double) course.completedHours() / course.totalHours() : 0.0;
        ProgressBar pBar = new ProgressBar(Math.clamp(progressRatio, 0.0, 1.0));
        pBar.setMaxWidth(Double.MAX_VALUE);
        pBar.setPrefHeight(6);
        pBar.setStyle("-fx-accent: #064E3B; -fx-control-inner-background: #E2E8F0;");

        VBox progressBox = new VBox(6, progressTextRow, pBar);

        // Botón dinámico según el rol y tipo de badge
        Button actionBtn = new Button();
        actionBtn.setMaxWidth(Double.MAX_VALUE);
        actionBtn.setPrefHeight(38);

        boolean isClassTodayTeacher = "Clase Hoy".equalsIgnoreCase(course.badgeType()) && isTeacherRole;

        if (isClassTodayTeacher) {
            actionBtn.setText("Tomar Asistencia ➔");
            actionBtn.setStyle("-fx-background-color: #064E3B; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 8px; -fx-cursor: hand;");
        } else {
            actionBtn.setText("Ver Curso ➔");
            actionBtn.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 12px; -fx-background-radius: 8px; -fx-cursor: hand;");
        }

        if (onAction != null) {
            actionBtn.setOnAction(e -> onAction.accept(course));
        }

        getChildren().addAll(topRow, courseName, infoRow, progressBox, actionBtn);
    }

    private void applyBadgeStyle(Label badge, String type) {
        badge.setPadding(new Insets(3, 10, 3, 10));
        if ("Clase Hoy".equalsIgnoreCase(type)) {
            badge.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-background-radius: 12px; -fx-font-size: 10px; -fx-font-weight: bold;");
        } else if ("Laboratorio".equalsIgnoreCase(type)) {
            badge.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; -fx-background-radius: 12px; -fx-font-size: 10px; -fx-font-weight: bold;");
        } else {
            badge.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #475569; -fx-background-radius: 12px; -fx-font-size: 10px; -fx-font-weight: bold;");
        }
    }

    public CourseModel getCourse() {
        return course;
    }

    public boolean isTeacherRole() {
        return isTeacherRole;
    }
}

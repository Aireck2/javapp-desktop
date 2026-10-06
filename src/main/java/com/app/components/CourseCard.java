package com.app.components;

import com.app.models.CourseModel;
import java.util.function.Consumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
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
    getStyleClass().add("course-card");

    // Header de la tarjeta: Código/Sección + Badge
    Label codeLabel = new Label(course.code() + " • SECCIÓN " + course.section());
    codeLabel.getStyleClass().add("course-card-code");

    Label badge = new Label(course.badgeType());
    applyBadgeStyle(badge, course.badgeType());

    Region topSpacer = new Region();
    HBox.setHgrow(topSpacer, Priority.ALWAYS);
    HBox topRow = new HBox(codeLabel, topSpacer, badge);
    topRow.setAlignment(Pos.CENTER_LEFT);

    // Título del curso
    Label courseName = new Label(course.name());
    courseName.getStyleClass().add("course-card-name");
    courseName.setWrapText(true);

    // Info de horario y aula con íconos
    HBox infoRow = new HBox(16);
    infoRow.setAlignment(Pos.CENTER_LEFT);

    FontIcon clockIcon = new FontIcon(Feather.CLOCK);
    clockIcon.setIconSize(13);
    clockIcon.getStyleClass().add("course-card-info-icon");
    Label scheduleLabel = new Label(course.schedule());
    scheduleLabel.getStyleClass().add("course-card-info-label");
    HBox scheduleBox = new HBox(5, clockIcon, scheduleLabel);
    scheduleBox.setAlignment(Pos.CENTER_LEFT);

    FontIcon roomIcon = new FontIcon(Feather.MAP_PIN);
    roomIcon.setIconSize(13);
    roomIcon.getStyleClass().add("course-card-info-icon");
    Label roomLabel = new Label(course.classroom());
    roomLabel.getStyleClass().add("course-card-info-label");
    HBox roomBox = new HBox(5, roomIcon, roomLabel);
    roomBox.setAlignment(Pos.CENTER_LEFT);

    infoRow.getChildren().addAll(scheduleBox, roomBox);

    // Barra de avance
    String studentsText = course.enrolledStudents() + (isTeacherRole ? " alumnos" : " compañeros");
    Label studentsLabel = new Label(studentsText);
    studentsLabel.getStyleClass().add("course-card-progress-label");

    String hoursText =
        course.completedHours()
            + "/"
            + course.totalHours()
            + " h ("
            + course.getProgressPercentage()
            + "%)";
    Label hoursLabel = new Label(hoursText);
    hoursLabel.getStyleClass().add("course-card-hours-label");

    Region progressSpacer = new Region();
    HBox.setHgrow(progressSpacer, Priority.ALWAYS);
    HBox progressTextRow = new HBox(studentsLabel, progressSpacer, hoursLabel);
    progressTextRow.setAlignment(Pos.CENTER_LEFT);

    double progressRatio =
        course.totalHours() > 0 ? (double) course.completedHours() / course.totalHours() : 0.0;
    ProgressBar pBar = new ProgressBar(Math.clamp(progressRatio, 0.0, 1.0));
    pBar.setMaxWidth(Double.MAX_VALUE);
    pBar.setPrefHeight(6);
    pBar.getStyleClass().add("course-card-progress");

    VBox progressBox = new VBox(6, progressTextRow, pBar);

    // Botón dinámico según el rol y tipo de badge
    Button actionBtn = new Button();
    actionBtn.setMaxWidth(Double.MAX_VALUE);
    actionBtn.setPrefHeight(38);

    boolean isClassTodayTeacher = "Clase Hoy".equalsIgnoreCase(course.badgeType()) && isTeacherRole;

    if (isClassTodayTeacher) {
      actionBtn.setText("Tomar Asistencia ➔");
      actionBtn.getStyleClass().add("course-action-today");
    } else {
      actionBtn.setText("Ver Curso ➔");
      actionBtn.getStyleClass().add("course-action-default");
    }

    if (onAction != null) {
      actionBtn.setOnAction(e -> onAction.accept(course));
    }

    getChildren().addAll(topRow, courseName, infoRow, progressBox, actionBtn);
  }

  private void applyBadgeStyle(Label badge, String type) {
    badge.getStyleClass().add("course-badge");
    if ("Clase Hoy".equalsIgnoreCase(type)) {
      badge.getStyleClass().add("course-badge-today");
    } else if ("Laboratorio".equalsIgnoreCase(type)) {
      badge.getStyleClass().add("course-badge-lab");
    } else {
      badge.getStyleClass().add("course-badge-default");
    }
  }

  public CourseModel getCourse() {
    return course;
  }

  public boolean isTeacherRole() {
    return isTeacherRole;
  }
}

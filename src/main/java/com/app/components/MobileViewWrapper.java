package com.app.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Contenedor global que simula el marco de un dispositivo móvil (420x780 px)
 * con esquinas redondeadas, sombra proyectada y barra de estado simulada.
 */
public class MobileViewWrapper extends StackPane {

  public static final double DEVICE_WIDTH = 720.0;
  public static final double DEVICE_HEIGHT = 1280.0;
  public static final double BORDER_RADIUS = 32.0;

  private final StackPane deviceFrame;
  private final StackPane contentArea;

  public MobileViewWrapper(Node rootContent) {
    // Fondo exterior de escritorio moderno
    setStyle("-fx-background-color: #0F172A;");
    setAlignment(Pos.CENTER);
    setPadding(new Insets(20));

    // Marco del teléfono
    deviceFrame = new StackPane();
    deviceFrame.setPrefSize(DEVICE_WIDTH, DEVICE_HEIGHT);
    deviceFrame.setMinSize(DEVICE_WIDTH, DEVICE_HEIGHT);
    deviceFrame.setMaxSize(DEVICE_WIDTH, DEVICE_HEIGHT);

    deviceFrame.setStyle(
        "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: " + BORDER_RADIUS + "px;" +
            "-fx-border-color: #334155;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: " + BORDER_RADIUS + "px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.35), 28, 0.15, 0, 12);");

    // Clip rectangular para asegurar esquinas redondeadas en todo el contenido hijo
    Rectangle clip = new Rectangle(DEVICE_WIDTH, DEVICE_HEIGHT);
    clip.setArcWidth(BORDER_RADIUS * 2);
    clip.setArcHeight(BORDER_RADIUS * 2);
    deviceFrame.setClip(clip);

    // Área central de contenido
    contentArea = new StackPane();
    if (rootContent != null) {
      contentArea.getChildren().add(rootContent);
    }

    // Barra de estado móvil simulada
    HBox statusBar = createStatusBar();

    VBox deviceLayout = new VBox();
    deviceLayout.getChildren().addAll(statusBar, contentArea);
    VBox.setVgrow(contentArea, Priority.ALWAYS);

    deviceFrame.getChildren().add(deviceLayout);
    getChildren().add(deviceFrame);
  }

  private HBox createStatusBar() {
    HBox statusBar = new HBox(8);
    statusBar.setAlignment(Pos.CENTER);
    statusBar.setPadding(new Insets(8, 20, 4, 20));
    statusBar.setStyle("-fx-background-color: transparent;");

    Label timeLabel = new Label("09:41");
    timeLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #475569;");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    // Indicadores simulados (notch / speaker pill + wifi dots)
    HBox notch = new HBox(4);
    notch.setAlignment(Pos.CENTER);
    notch.setPrefWidth(60);
    notch.setPrefHeight(6);
    notch.setStyle("-fx-background-color: #CBD5E1; -fx-background-radius: 6px;");

    HBox icons = new HBox(4);
    icons.setAlignment(Pos.CENTER);
    Circle dot1 = new Circle(2.5, Color.web("#94A3B8"));
    Circle dot2 = new Circle(2.5, Color.web("#94A3B8"));
    Circle dot3 = new Circle(2.5, Color.web("#475569"));
    icons.getChildren().addAll(dot1, dot2, dot3);

    statusBar.getChildren().addAll(timeLabel, spacer, notch, new Region(), icons);
    return statusBar;
  }

  public void setContent(Node content) {
    contentArea.getChildren().setAll(content);
  }

  public StackPane getContentArea() {
    return contentArea;
  }

  public StackPane getDeviceFrame() {
    return deviceFrame;
  }
}

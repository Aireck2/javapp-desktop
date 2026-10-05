package com.app.components;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Cabecera institucional compartida entre los portales Docente y Alumno.
 */
public class AppHeader extends HBox {

    private Runnable onAvatarClick;
    private final Label subtitleLabel;

    public AppHeader(String roleSubtitle) {
        this(roleSubtitle, null);
    }

    public AppHeader(String roleSubtitle, Runnable onAvatarClick) {
        this.onAvatarClick = onAvatarClick;

        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(12, 16, 12, 16));
        setSpacing(10);
        setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 0 0 1 0;");

        ImageView appIcon = new ImageView(new Image(getClass().getResourceAsStream("/images/logo.png")));
        appIcon.setFitWidth(34);
        appIcon.setFitHeight(34);
        appIcon.setPreserveRatio(true);
        StackPane iconBox = new StackPane(appIcon);

        Label title = new Label("Mis Cursos •");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0F172A;");

        this.subtitleLabel = new Label(roleSubtitle != null ? roleSubtitle : "PORTAL");
        this.subtitleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B; -fx-font-weight: bold;");

        VBox titleBox = new VBox(2, title, subtitleLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Avatar usuario
        FontIcon userIcon = new FontIcon(Feather.USER);
        userIcon.setIconColor(Color.WHITE);
        userIcon.setIconSize(14);

        Button avatarBtn = new Button("", userIcon);
        avatarBtn.setStyle(
                "-fx-background-color: #064E3B; -fx-background-radius: 50%; -fx-cursor: hand; -fx-padding: 8px;");
        MenuItem logoutItem = new MenuItem("Cerrar sesión");
        logoutItem.setOnAction(e -> {
            if (this.onAvatarClick != null) {
                this.onAvatarClick.run();
            }
        });
        ContextMenu avatarMenu = new ContextMenu(logoutItem);
        avatarBtn.setOnAction(e -> avatarMenu.show(avatarBtn, javafx.geometry.Side.BOTTOM, 0, 4));

        getChildren().addAll(iconBox, titleBox, spacer, avatarBtn);
    }

    public void setSubtitle(String subtitle) {
        this.subtitleLabel.setText(subtitle != null ? subtitle : "PORTAL");
    }

    public String getSubtitle() {
        return this.subtitleLabel.getText();
    }

    public void setOnAvatarClick(Runnable onAvatarClick) {
        this.onAvatarClick = onAvatarClick;
    }
}

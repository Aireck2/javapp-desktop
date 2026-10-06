package com.app.components;

import com.app.shared.fxml.FxmlViewLoader;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/** Cabecera institucional compartida entre los portales Docente y Alumno. */
public class AppHeader extends HBox {

  private Runnable onAvatarClick;
  @FXML private ImageView appIcon;
  @FXML private Label subtitleLabel;
  @FXML private Button avatarButton;

  public AppHeader(String roleSubtitle) {
    this(roleSubtitle, null);
  }

  public AppHeader(String roleSubtitle, Runnable onAvatarClick) {
    this.onAvatarClick = onAvatarClick;
    FxmlViewLoader.loadInto(this, "/com/app/shared/components/app-header.fxml", this);
    appIcon.setImage(new Image(getClass().getResourceAsStream("/images/logo.png")));
    subtitleLabel.setText(roleSubtitle != null ? roleSubtitle : "PORTAL");
    FontIcon userIcon = new FontIcon(Feather.USER);
    userIcon.setIconSize(14);
    userIcon.getStyleClass().add("portal-avatar-icon");
    avatarButton.setGraphic(userIcon);
    MenuItem logoutItem = new MenuItem("Cerrar sesión");
    logoutItem.setOnAction(
        e -> {
          if (this.onAvatarClick != null) {
            this.onAvatarClick.run();
          }
        });
    ContextMenu avatarMenu = new ContextMenu(logoutItem);
    avatarMenu.setOnShown(event -> keepMenuInsideWindow(avatarMenu));
    avatarButton.setOnAction(e -> avatarMenu.show(avatarButton, javafx.geometry.Side.BOTTOM, 0, 4));
  }

  private void keepMenuInsideWindow(ContextMenu menu) {
    if (avatarButton.getScene() == null || menu.getWidth() <= 0 || menu.getHeight() <= 0) {
      return;
    }

    Bounds windowBounds =
        avatarButton
            .getScene()
            .getRoot()
            .localToScreen(avatarButton.getScene().getRoot().getBoundsInLocal());
    Bounds buttonBounds = avatarButton.localToScreen(avatarButton.getBoundsInLocal());
    if (windowBounds == null || buttonBounds == null) {
      return;
    }

    double maxX = Math.max(windowBounds.getMinX(), windowBounds.getMaxX() - menu.getWidth());
    double maxY = Math.max(windowBounds.getMinY(), windowBounds.getMaxY() - menu.getHeight());
    double x =
        Math.max(windowBounds.getMinX(), Math.min(buttonBounds.getMaxX() - menu.getWidth(), maxX));
    double y = Math.max(windowBounds.getMinY(), Math.min(buttonBounds.getMaxY() + 4, maxY));
    menu.setX(x);
    menu.setY(y);
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

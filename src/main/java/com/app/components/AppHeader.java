package com.app.components;

import com.app.shared.fxml.FxmlViewLoader;
import javafx.fxml.FXML;
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
    avatarButton.setOnAction(e -> avatarMenu.show(avatarButton, javafx.geometry.Side.BOTTOM, 0, 4));
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

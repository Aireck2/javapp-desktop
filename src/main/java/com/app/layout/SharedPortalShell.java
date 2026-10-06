package com.app.layout;

import com.app.components.AppHeader;
import com.app.components.BottomNavBar;
import com.app.shared.fxml.FxmlViewLoader;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

/**
 * Shell de layout compartido para portales institucional y móvil. Estructura: Top (AppHeader),
 * Center (ScrollPane con vista inyectada), Bottom (BottomNavBar).
 */
public class SharedPortalShell extends BorderPane {

  private final AppHeader header;
  private final BottomNavBar bottomNavBar;

  @FXML private StackPane headerHost;
  @FXML private StackPane bottomHost;
  @FXML private ScrollPane scrollContent;

  public SharedPortalShell(String portalTypeTitle, boolean isTeacher) {
    this(portalTypeTitle, isTeacher, null, null);
  }

  public SharedPortalShell(
      String portalTypeTitle,
      boolean isTeacher,
      Runnable onAvatarClick,
      BottomNavBar.TabItem initialTab) {

    this.header = new AppHeader(portalTypeTitle, onAvatarClick);
    this.bottomNavBar = new BottomNavBar(isTeacher);
    if (initialTab != null) {
      this.bottomNavBar.setActiveTab(initialTab);
    }
    FxmlViewLoader.loadInto(this, "/com/app/shared/components/shared-portal-shell.fxml", this);
    this.headerHost.getChildren().setAll(header);
    this.bottomHost.getChildren().setAll(bottomNavBar);
  }

  /**
   * Inyecta el contenido dinámico en el área scrolleable central.
   *
   * @param node Nodo de la vista a mostrar
   */
  public void setViewContent(Node node) {
    scrollContent.setContent(node);
  }

  public void setPortalTitle(String title) {
    header.setSubtitle(title);
  }

  public AppHeader getHeader() {
    return header;
  }

  public ScrollPane getScrollContent() {
    return scrollContent;
  }

  public BottomNavBar getBottomNavBar() {
    return bottomNavBar;
  }
}

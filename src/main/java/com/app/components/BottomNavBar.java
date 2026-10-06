package com.app.components;

import com.app.shared.fxml.FxmlViewLoader;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/** Barra de navegación inferior compartida (Bottom Bar) para portales Docente y Alumno. */
public class BottomNavBar extends HBox {

  public enum TabItem {
    ATTENDANCE,
    SCHEDULE,
    PROFILE
  }

  private final boolean isTeacher;
  private Consumer<TabItem> onTabSelected;
  private TabItem currentActiveTab = TabItem.ATTENDANCE;

  @FXML private VBox attendanceItem;
  @FXML private VBox profileItem;
  @FXML private Label attendanceIconPlaceholder;
  @FXML private Label profileIconPlaceholder;
  @FXML private Label attendanceLabel;
  @FXML private Label profileLabel;

  public BottomNavBar(boolean isTeacher) {
    this(isTeacher, null);
  }

  public BottomNavBar(boolean isTeacher, Consumer<TabItem> onTabSelected) {
    this.isTeacher = isTeacher;
    this.onTabSelected = onTabSelected;
    FxmlViewLoader.loadInto(this, "/com/app/shared/components/bottom-nav-bar.fxml", this);
    attendanceLabel.setText(isTeacher ? "Mis cursos" : "Inicio");
    attendanceIconPlaceholder.setGraphic(new FontIcon(Feather.BOOK_OPEN));
    profileIconPlaceholder.setGraphic(new FontIcon(Feather.USER));
    attendanceItem.setOnMouseClicked(e -> select(TabItem.ATTENDANCE));
    profileItem.setOnMouseClicked(e -> select(TabItem.PROFILE));
    updateActiveState();
  }

  public void setOnTabSelected(Consumer<TabItem> onTabSelected) {
    this.onTabSelected = onTabSelected;
  }

  private void select(TabItem tab) {
    setActiveTab(tab);
    if (onTabSelected != null) {
      onTabSelected.accept(tab);
    }
  }

  public void setActiveTab(TabItem tab) {
    this.currentActiveTab = tab;
    updateActiveState();
  }

  private void updateActiveState() {
    styleItem(
        attendanceItem,
        attendanceIconPlaceholder,
        attendanceLabel,
        currentActiveTab == TabItem.ATTENDANCE);
    styleItem(
        profileItem, profileIconPlaceholder, profileLabel, currentActiveTab == TabItem.PROFILE);
  }

  private static void styleItem(VBox item, Label iconLabel, Label label, boolean active) {
    String color = active ? "#064E3B" : "#94A3B8";
    ((FontIcon) iconLabel.getGraphic()).setIconColor(Color.web(color));
    label.setTextFill(Color.web(color));
    item.getStyleClass().removeAll("nav-item-active");
    if (active) item.getStyleClass().add("nav-item-active");
  }

  public TabItem getCurrentActiveTab() {
    return currentActiveTab;
  }

  public boolean isTeacher() {
    return isTeacher;
  }
}

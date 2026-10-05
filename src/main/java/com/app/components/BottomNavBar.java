package com.app.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Barra de navegación inferior compartida (Bottom Bar) para portales Docente y
 * Alumno.
 */
public class BottomNavBar extends HBox {

    public enum TabItem {
        ATTENDANCE,
        SCHEDULE,
        PROFILE
    }

    private final boolean isTeacher;
    private Consumer<TabItem> onTabSelected;
    private final List<VBox> navBoxes = new ArrayList<>();
    private TabItem currentActiveTab = TabItem.ATTENDANCE;

    public BottomNavBar(boolean isTeacher) {
        this(isTeacher, null);
    }

    public BottomNavBar(boolean isTeacher, Consumer<TabItem> onTabSelected) {
        this.isTeacher = isTeacher;
        this.onTabSelected = onTabSelected;

        setAlignment(Pos.CENTER);
        setStyle(
                "-fx-background-color: #FFFFFF; -fx-border-color: #E2E8F0; -fx-border-width: 1px 0 0 0; -fx-padding: 8 0 8 0;");

        buildNavItems();
    }

    public void setOnTabSelected(Consumer<TabItem> onTabSelected) {
        this.onTabSelected = onTabSelected;
    }

    private void buildNavItems() {
        // UserSession session = UserSession.getInstance();
        // boolean isDocente = session.hasRole("DOCENTE");
        // boolean isAlumno = session.hasRole("ALUMNO");
        // boolean isAdmin = session.hasRole("ADMIN");
        getChildren().clear();
        navBoxes.clear();

        addNavItem(TabItem.ATTENDANCE, Feather.BOOK_OPEN, isTeacher ? "Mis cursos" : "Inicio");
        addNavItem(TabItem.PROFILE, Feather.USER, "Perfil");

        updateActiveState();
    }

    private void addNavItem(TabItem tab, Feather featherIcon, String text) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setStyle("-fx-cursor: hand; -fx-padding: 4 8 4 8;");

        FontIcon icon = new FontIcon(featherIcon);
        icon.setIconSize(18);

        Label label = new Label(text);

        box.getChildren().addAll(icon, label);
        box.setUserData(tab);

        box.setOnMouseClicked(e -> {
            setActiveTab(tab);
            if (onTabSelected != null) {
                onTabSelected.accept(tab);
            }
        });

        navBoxes.add(box);
        getChildren().add(box);
    }

    public void setActiveTab(TabItem tab) {
        this.currentActiveTab = tab;
        updateActiveState();
    }

    private void updateActiveState() {
        for (VBox box : navBoxes) {
            TabItem tab = (TabItem) box.getUserData();
            boolean isActive = tab == currentActiveTab;

            String colorHex = isActive ? "#064E3B" : "#94A3B8";
            FontIcon icon = (FontIcon) box.getChildren().get(0);
            Label label = (Label) box.getChildren().get(1);

            icon.setIconColor(Color.web(colorHex));
            label.setStyle("-fx-font-size: 11px; -fx-text-fill: " + colorHex + "; -fx-font-weight: "
                    + (isActive ? "bold" : "normal") + ";");
        }
    }

    public TabItem getCurrentActiveTab() {
        return currentActiveTab;
    }

    public boolean isTeacher() {
        return isTeacher;
    }
}

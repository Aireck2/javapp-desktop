package com.app.layout;

import com.app.components.AppHeader;
import com.app.components.BottomNavBar;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;

/**
 * Shell de layout compartido para portales institucional y móvil.
 * Estructura: Top (AppHeader), Center (ScrollPane con vista inyectada), Bottom (BottomNavBar).
 */
public class SharedPortalShell extends BorderPane {

    private final AppHeader header;
    private final ScrollPane scrollContent;
    private final BottomNavBar bottomNavBar;

    public SharedPortalShell(String portalTypeTitle, boolean isTeacher) {
        this(portalTypeTitle, isTeacher, null, null);
    }

    public SharedPortalShell(
            String portalTypeTitle,
            boolean isTeacher,
            Runnable onAvatarClick,
            BottomNavBar.TabItem initialTab) {

        this.header = new AppHeader(portalTypeTitle, onAvatarClick);
        setTop(header);

        this.scrollContent = new ScrollPane();
        this.scrollContent.setFitToWidth(true);
        this.scrollContent.setStyle("-fx-background-color: #F8FAFC; -fx-background: #F8FAFC; -fx-border-color: transparent;");
        setCenter(scrollContent);

        this.bottomNavBar = new BottomNavBar(isTeacher);
        if (initialTab != null) {
            this.bottomNavBar.setActiveTab(initialTab);
        }
        setBottom(bottomNavBar);
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

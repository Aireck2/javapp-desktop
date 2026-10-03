package com.app.navigation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Enrutador desacoplado para gestión de pantallas y transiciones en JavaFX.
 * Mantiene un historial de navegación (LIFO) y soporta paso de parámetros.
 */
public class ScreenRouter {

    private final StackPane container;
    private final Map<String, Function<Object, Node>> routes = new HashMap<>();
    private final Deque<HistoryEntry> history = new ArrayDeque<>();
    private String currentRoute;
    private Object currentData;

    private static final Duration FADE_OUT_DURATION = Duration.millis(120);
    private static final Duration FADE_IN_DURATION = Duration.millis(180);

    public record HistoryEntry(String route, Object data) {}

    public ScreenRouter(StackPane container) {
        this.container = Objects.requireNonNull(container, "El contenedor StackPane no puede ser nulo");
    }

    /**
     * Registra una vista con una fábrica que acepta un parámetro de datos.
     *
     * @param route   Nombre único de la ruta (ej. "login", "main", "courses")
     * @param factory Función constructora que recibe un objeto y retorna un nodo JavaFX
     */
    public void registerView(String route, Function<Object, Node> factory) {
        routes.put(route, Objects.requireNonNull(factory, "El factory no puede ser nulo"));
    }

    /**
     * Navega a una ruta registrada sin pasar datos adicionales.
     */
    public void navigateTo(String route) {
        navigateTo(route, null);
    }

    /**
     * Navega a una ruta pasando un objeto de parámetros con animación fluida.
     *
     * @param route Identificador de la ruta de destino
     * @param data  Parámetros arbitrarios para la vista receptora
     */
    public void navigateTo(String route, Object data) {
        if (!routes.containsKey(route)) {
            throw new IllegalArgumentException("Ruta no registrada en ScreenRouter: " + route);
        }

        if (currentRoute != null) {
            history.push(new HistoryEntry(currentRoute, currentData));
        }

        renderRoute(route, data);
    }

    /**
     * Regresa a la pantalla previa en la pila de historial.
     *
     * @return true si se realizó la navegación hacia atrás, false si la pila estaba vacía
     */
    public boolean goBack() {
        if (history.isEmpty()) {
            return false;
        }

        HistoryEntry previous = history.pop();
        renderRoute(previous.route(), previous.data());
        return true;
    }

    /**
     * Realiza el cambio de vista con animación FadeTransition.
     */
    private void renderRoute(String route, Object data) {
        this.currentRoute = route;
        this.currentData = data;

        Function<Object, Node> factory = routes.get(route);
        Node newView = factory.apply(data);

        if (container.getChildren().isEmpty()) {
            container.getChildren().setAll(newView);
            FadeTransition fadeIn = new FadeTransition(FADE_IN_DURATION, newView);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        } else {
            Node currentView = container.getChildren().get(0);
            FadeTransition fadeOut = new FadeTransition(FADE_OUT_DURATION, currentView);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                container.getChildren().setAll(newView);
                newView.setOpacity(0.0);
                FadeTransition fadeIn = new FadeTransition(FADE_IN_DURATION, newView);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        }
    }

    public String getCurrentRoute() {
        return currentRoute;
    }

    public Object getCurrentData() {
        return currentData;
    }

    public boolean canGoBack() {
        return !history.isEmpty();
    }

    public void clearHistory() {
        history.clear();
    }

    public StackPane getContainer() {
        return container;
    }
}

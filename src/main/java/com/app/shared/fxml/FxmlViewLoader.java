package com.app.shared.fxml;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.function.Function;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/** Loads an FXML view with explicit controller construction and app CSS. */
public final class FxmlViewLoader {

  private static final String APP_STYLESHEET = "/com/app/css/theme.css";

  private FxmlViewLoader() {}

  public static Parent load(String resourcePath, Function<Class<?>, Object> controllerFactory) {
    Objects.requireNonNull(resourcePath, "resourcePath");
    Objects.requireNonNull(controllerFactory, "controllerFactory");

    URL resource = FxmlViewLoader.class.getResource(resourcePath);
    if (resource == null) {
      throw new IllegalArgumentException("FXML resource not found: " + resourcePath);
    }

    URL stylesheet = FxmlViewLoader.class.getResource(APP_STYLESHEET);
    if (stylesheet == null) {
      throw new IllegalStateException("Application stylesheet not found: " + APP_STYLESHEET);
    }

    FXMLLoader loader = new FXMLLoader(resource);
    loader.setControllerFactory(controllerFactory::apply);
    try {
      Parent view = loader.load();
      view.getStylesheets().add(stylesheet.toExternalForm());
      return view;
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to load FXML resource: " + resourcePath, exception);
    }
  }

  /** Loads an {@code <fx:root>} document into an existing custom JavaFX node. */
  public static <T extends Parent> T loadInto(T root, String resourcePath, Object controller) {
    Objects.requireNonNull(root, "root");
    Objects.requireNonNull(controller, "controller");
    Objects.requireNonNull(resourcePath, "resourcePath");

    URL resource = FxmlViewLoader.class.getResource(resourcePath);
    if (resource == null) {
      throw new IllegalArgumentException("FXML resource not found: " + resourcePath);
    }

    URL stylesheet = FxmlViewLoader.class.getResource(APP_STYLESHEET);
    if (stylesheet == null) {
      throw new IllegalStateException("Application stylesheet not found: " + APP_STYLESHEET);
    }

    FXMLLoader loader = new FXMLLoader(resource);
    loader.setRoot(root);
    loader.setController(controller);
    try {
      loader.load();
      root.getStylesheets().add(stylesheet.toExternalForm());
      return root;
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to load FXML resource: " + resourcePath, exception);
    }
  }
}

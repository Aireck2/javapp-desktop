package com.app.features.profile;

import com.app.auth.UserSession;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Presents the signed-in identity on the profile screen. */
public final class ProfileController {

  private final UserSession session;

  @FXML private Label displayName;
  @FXML private Label username;
  @FXML private Label roles;

  public ProfileController(UserSession session) {
    this.session = session;
  }

  @FXML
  private void initialize() {
    displayName.setText(valueOrUnavailable(session.getDisplayName()));
    username.setText(valueOrUnavailable(session.getUsername()));
    roles.setText(session.getGroups().stream().sorted().collect(Collectors.joining(", ")));
    if (roles.getText().isBlank()) {
      roles.setText("No disponible");
    }
  }

  private static String valueOrUnavailable(String value) {
    return value == null || value.isBlank() ? "No disponible" : value;
  }
}

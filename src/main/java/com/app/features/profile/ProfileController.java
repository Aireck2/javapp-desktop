package com.app.features.profile;

import com.app.auth.UserSession;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Presents the signed-in identity on the profile screen. */
public final class ProfileController {

  private final UserSession session;

  @FXML private Label displayName;
  @FXML private Label fullName;
  @FXML private Label username;
  @FXML private Label avatarInitials;
  @FXML private Label profileSubtitle;
  @FXML private Label primaryRole;
  @FXML private Label roles;

  public ProfileController(UserSession session) {
    this.session = session;
  }

  @FXML
  private void initialize() {
    String name = valueOrUnavailable(session.getDisplayName());
    Set<String> groups = session.getGroups();
    displayName.setText(name);
    fullName.setText(name);
    username.setText(valueOrUnavailable(session.getUsername()));
    avatarInitials.setText(initials(name));

    String roleNames = groups.stream().sorted().collect(Collectors.joining(" · "));
    roles.setText(roleNames.isBlank() ? "Rol no disponible" : roleNames);

    String mainRole = primaryRole(groups);
    primaryRole.setText(mainRole);
    profileSubtitle.setText(
        "Perfil de "
            + (mainRole.equals("Rol no disponible")
                ? "usuario"
                : mainRole.toLowerCase(Locale.ROOT)));
  }

  private static String primaryRole(Set<String> groups) {
    if (containsRole(groups, "DOCENTE")) {
      return "Docente";
    }
    if (containsRole(groups, "ALUMNO")) {
      return "Alumno";
    }
    if (containsRole(groups, "ADMIN")) {
      return "Administrador";
    }
    return "Rol no disponible";
  }

  private static boolean containsRole(Set<String> groups, String role) {
    return groups.stream().anyMatch(group -> group.equalsIgnoreCase(role));
  }

  private static String initials(String name) {
    if (name == null || name.isBlank() || name.equals("No disponible")) {
      return "?";
    }
    String[] parts = name.trim().split("\\s+");
    String first = parts[0].substring(0, 1);
    String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
    return (first + last).toUpperCase(Locale.ROOT);
  }

  private static String valueOrUnavailable(String value) {
    return value == null || value.isBlank() ? "No disponible" : value;
  }
}

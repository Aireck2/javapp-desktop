package com.app.features.auth;

import com.app.api.dto.LoginRequest;
import com.app.api.dto.SessionTokens;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Handles login form events and session feedback. */
public final class LoginController {

  @FXML private ImageView logo;
  @FXML private TextField usernameField;
  @FXML private PasswordField passwordField;
  @FXML private CheckBox rememberMe;
  @FXML private Label errorLabel;
  @FXML private Button loginButton;

  private final LoginViewModel viewModel;
  private final Consumer<SessionTokens> onSuccess;

  public LoginController(LoginViewModel viewModel, Consumer<SessionTokens> onSuccess) {
    this.viewModel = viewModel;
    this.onSuccess = onSuccess;
  }

  @FXML
  private void initialize() {
    loadLogo();
    loginButton.setOnAction(event -> login());
    passwordField.setOnAction(event -> login());
    usernameField.setOnAction(event -> passwordField.requestFocus());
  }

  private void loadLogo() {
    var stream = getClass().getResourceAsStream("/images/logo.png");
    if (stream != null) {
      logo.setImage(new Image(stream));
    }
  }

  private void login() {
    errorLabel.setText("");
    String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
    String password = passwordField.getText() == null ? "" : passwordField.getText();
    if (username.isEmpty() || password.isEmpty()) {
      errorLabel.setText("Por favor ingrese usuario y contraseña");
      return;
    }

    boolean persistSession = rememberMe.isSelected();
    LoginRequest request = new LoginRequest(username, password);
    viewModel.login(
        request,
        persistSession,
        state -> {
          if (state.loading()) {
            loginButton.setDisable(true);
            loginButton.setText("Ingresando…");
            return;
          }
          Platform.runLater(
              () -> {
                loginButton.setDisable(false);
                loginButton.setText("➔ Iniciar Sesión");
                if (state.tokens() != null) {
                  if (onSuccess != null) {
                    onSuccess.accept(state.tokens());
                  }
                } else {
                  showError(state.errorMessage());
                }
              });
        });
  }

  private void showError(String message) {
    errorLabel.setText(message);
    loginButton.setDisable(false);
    loginButton.setText("➔ Iniciar Sesión");
  }
}

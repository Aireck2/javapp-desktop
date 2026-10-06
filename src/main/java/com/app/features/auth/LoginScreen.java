package com.app.features.auth;

import com.app.api.ApiClient;
import com.app.api.dto.SessionTokens;
import com.app.auth.TokenStore;
import com.app.shared.fxml.FxmlViewLoader;
import java.util.function.Consumer;
import javafx.scene.layout.VBox;

/** FXML-backed login screen. */
public class LoginScreen extends VBox {

  public LoginScreen(ApiClient api, TokenStore tokenStore, Consumer<SessionTokens> onSuccess) {
    LoginViewModel viewModel = new LoginViewModel(new AuthenticationService(api, tokenStore));
    FxmlViewLoader.loadInto(
        this, "/com/app/features/auth/login.fxml", new LoginController(viewModel, onSuccess));
  }
}

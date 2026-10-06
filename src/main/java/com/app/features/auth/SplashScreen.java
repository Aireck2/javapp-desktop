package com.app.features.auth;

import com.app.api.ApiClient;
import com.app.api.dto.SessionTokens;
import com.app.auth.TokenStore;
import com.app.shared.fxml.FxmlViewLoader;
import java.util.function.Consumer;
import javafx.scene.layout.VBox;

/** FXML-backed splash screen. */
public class SplashScreen extends VBox {

  public SplashScreen(
      ApiClient api,
      TokenStore tokenStore,
      Consumer<SessionTokens> onSuccess,
      Runnable onLoginRequired) {
    SplashViewModel viewModel = new SplashViewModel(new AuthenticationService(api, tokenStore));
    FxmlViewLoader.loadInto(
        this,
        "/com/app/features/auth/splash.fxml",
        new SplashController(viewModel, onSuccess, onLoginRequired));
  }
}

package com.app.views;

import com.app.api.ApiClient;
import com.app.api.dto.SessionTokens;
import com.app.auth.TokenStore;
import com.app.navigation.ScreenRouter;
import java.util.function.Consumer;

/**
 * Delegado de compatibilidad en el paquete com.app.views para SplashView.
 */
public class SplashView extends com.app.auth.SplashView {

    public SplashView(ScreenRouter router) {
        super(router);
    }

    public SplashView(
            ApiClient api,
            TokenStore tokenStore,
            Consumer<SessionTokens> onSuccess,
            Runnable onLoginRequired) {
        super(api, tokenStore, onSuccess, onLoginRequired);
    }
}

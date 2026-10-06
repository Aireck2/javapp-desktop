package com.app.features.auth;

import com.app.api.ApiClient;
import com.app.api.ApiException;
import com.app.api.dto.LoginRequest;
import com.app.api.dto.SessionTokens;
import com.app.auth.TokenStore;
import com.app.auth.UserSession;
import java.util.Optional;

/** Authentication use cases over the API contract and lightweight local session store. */
public final class AuthenticationService {

  private final ApiClient api;
  private final TokenStore tokenStore;

  public AuthenticationService(ApiClient api, TokenStore tokenStore) {
    this.api = api;
    this.tokenStore = tokenStore;
  }

  public SessionTokens login(LoginRequest request, boolean persistRefreshToken) {
    SessionTokens tokens = api.login(request);
    establishSession(tokens);
    if (persistRefreshToken) {
      tokenStore.saveRefreshToken(tokens.refreshToken());
    } else {
      tokenStore.clear();
    }
    return tokens;
  }

  public Optional<SessionTokens> restoreSession() {
    if (api == null || tokenStore == null) {
      return Optional.empty();
    }
    Optional<String> savedToken = tokenStore.loadRefreshToken();
    if (savedToken.isEmpty()) {
      return Optional.empty();
    }
    try {
      SessionTokens tokens = api.refresh(savedToken.get());
      establishSession(tokens);
      tokenStore.saveRefreshToken(tokens.refreshToken());
      return Optional.of(tokens);
    } catch (ApiException exception) {
      tokenStore.clear();
      throw exception;
    }
  }

  private static void establishSession(SessionTokens tokens) {
    UserSession session = UserSession.getInstance();
    session.login(
        tokens.accessToken(),
        tokens.username(),
        tokens.displayName(),
        tokens.groups(),
        tokens.expiresAt());
    session.touch();
  }
}

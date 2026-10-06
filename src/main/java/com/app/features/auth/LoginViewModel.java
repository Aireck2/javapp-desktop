package com.app.features.auth;

import com.app.api.ApiException;
import com.app.api.dto.LoginRequest;
import com.app.api.dto.SessionTokens;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/** Owns login request state and submits work asynchronously. */
public final class LoginViewModel {

  public record State(
      boolean loading, SessionTokens tokens, String errorMessage, boolean authenticationError) {}

  private final AuthenticationService service;

  public LoginViewModel(AuthenticationService service) {
    this.service = service;
  }

  public void login(LoginRequest request, boolean remember, Consumer<State> listener) {
    listener.accept(new State(true, null, "", false));
    CompletableFuture.supplyAsync(() -> service.login(request, remember))
        .whenComplete(
            (tokens, failure) -> {
              if (failure == null) {
                listener.accept(new State(false, tokens, "", false));
                return;
              }
              Throwable cause =
                  failure instanceof CompletionException && failure.getCause() != null
                      ? failure.getCause()
                      : failure;
              boolean authError = cause instanceof ApiException;
              listener.accept(
                  new State(
                      false,
                      null,
                      authError
                          ? "Usuario o contraseña incorrectos"
                          : "Error de comunicación: " + cause.getMessage(),
                      authError));
            });
  }
}

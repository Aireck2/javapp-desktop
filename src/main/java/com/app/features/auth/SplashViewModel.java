package com.app.features.auth;

import com.app.api.dto.SessionTokens;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Restores the local session asynchronously for the splash flow. */
public final class SplashViewModel {

  private final AuthenticationService service;

  public SplashViewModel(AuthenticationService service) {
    this.service = service;
  }

  public void restoreSession(Consumer<Optional<SessionTokens>> listener) {
    CompletableFuture.supplyAsync(service::restoreSession)
        .whenComplete(
            (tokens, failure) -> listener.accept(failure == null ? tokens : Optional.empty()));
  }
}

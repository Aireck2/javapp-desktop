package com.app.features.courses;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/** Holds load state for the course timeline independently of its controls. */
public final class CoursesViewModel {

  public enum Status {
    LOADING,
    CONTENT,
    EMPTY,
    ERROR
  }

  public record ScreenState(Status status, CoursesService.Timeline timeline, String message) {}

  private final CoursesService service;

  public CoursesViewModel(CoursesService service) {
    this.service = service;
  }

  public void load(Consumer<ScreenState> stateListener) {
    stateListener.accept(new ScreenState(Status.LOADING, null, ""));
    CompletableFuture.supplyAsync(service::loadTimeline)
        .whenComplete(
            (timeline, failure) -> {
              if (failure != null) {
                Throwable cause =
                    failure instanceof CompletionException && failure.getCause() != null
                        ? failure.getCause()
                        : failure;
                stateListener.accept(
                    new ScreenState(
                        Status.ERROR,
                        null,
                        cause.getMessage() == null
                            ? "No fue posible cargar las sesiones."
                            : cause.getMessage()));
              } else if (timeline.empty()) {
                stateListener.accept(
                    new ScreenState(
                        Status.EMPTY, timeline, "Sin sesiones próximas en los siguientes 5 días."));
              } else {
                stateListener.accept(new ScreenState(Status.CONTENT, timeline, ""));
              }
            });
  }
}

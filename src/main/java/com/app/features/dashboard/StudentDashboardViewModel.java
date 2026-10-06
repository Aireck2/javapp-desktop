package com.app.features.dashboard;

import com.app.models.StudentCourseModel;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/** Exposes the student's dashboard loading, content, empty and error states. */
public final class StudentDashboardViewModel {

  public enum Status {
    LOADING,
    CONTENT,
    EMPTY,
    ERROR
  }

  public record ScreenState(Status status, List<StudentCourseModel> courses, String message) {
    public ScreenState {
      courses = courses == null ? List.of() : List.copyOf(courses);
    }
  }

  private final StudentDashboardService service;

  public StudentDashboardViewModel(StudentDashboardService service) {
    this.service = service;
  }

  public void load(Consumer<ScreenState> listener) {
    listener.accept(new ScreenState(Status.LOADING, List.of(), ""));
    CompletableFuture.supplyAsync(service::load)
        .whenComplete(
            (courses, failure) -> {
              if (failure != null) {
                Throwable cause =
                    failure instanceof CompletionException && failure.getCause() != null
                        ? failure.getCause()
                        : failure;
                listener.accept(
                    new ScreenState(
                        Status.ERROR,
                        List.of(),
                        cause.getMessage() == null
                            ? "No fue posible cargar el resumen de asistencia."
                            : cause.getMessage()));
              } else if (courses.isEmpty()) {
                listener.accept(
                    new ScreenState(Status.EMPTY, List.of(), "No hay cursos para mostrar."));
              } else {
                listener.accept(new ScreenState(Status.CONTENT, courses, ""));
              }
            });
  }
}

package com.app.features.attendance;

import com.app.api.dto.ClassSession;
import com.app.api.dto.Course;
import com.app.models.StudentAttendanceModel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/** Coordinates asynchronous attendance loading and saving for the screen. */
public final class AttendanceDetailViewModel {

  public enum Status {
    LOADING,
    CONTENT,
    ERROR
  }

  public record ScreenState(
      Status status,
      Course course,
      List<StudentAttendanceModel> students,
      String observation,
      String message) {
    public ScreenState {
      students = students == null ? List.of() : List.copyOf(students);
    }
  }

  public record SaveState(List<StudentAttendanceModel> students, String error) {
    public SaveState {
      students = students == null ? List.of() : List.copyOf(students);
    }

    public boolean successful() {
      return error == null;
    }
  }

  private final AttendanceService service;
  private volatile Course course;
  private volatile List<StudentAttendanceModel> students = List.of();

  public AttendanceDetailViewModel(AttendanceService service) {
    this.service = service;
  }

  public void load(
      ClassSession session,
      Map<String, boolean[]> draftMarks,
      String draftObservation,
      Consumer<ScreenState> listener) {
    listener.accept(new ScreenState(Status.LOADING, null, List.of(), "", ""));
    CompletableFuture.supplyAsync(() -> service.load(session, draftMarks, draftObservation))
        .whenComplete(
            (data, failure) -> {
              if (failure != null) {
                listener.accept(
                    new ScreenState(Status.ERROR, null, List.of(), "", rootMessage(failure)));
              } else {
                course = data.course();
                students = data.students();
                listener.accept(
                    new ScreenState(
                        Status.CONTENT, data.course(), data.students(), data.observation(), ""));
              }
            });
  }

  public void save(
      ClassSession session,
      Course course,
      Map<String, boolean[]> marks,
      String observation,
      Consumer<SaveState> listener) {
    CompletableFuture.supplyAsync(() -> service.save(session, course, marks, observation))
        .whenComplete(
            (reloaded, failure) -> {
              if (failure == null) {
                students = reloaded;
                listener.accept(new SaveState(reloaded, null));
              } else {
                listener.accept(new SaveState(List.of(), rootMessage(failure)));
              }
            });
  }

  public Course course() {
    return course;
  }

  public List<StudentAttendanceModel> students() {
    return students;
  }

  private static String rootMessage(Throwable failure) {
    Throwable root = failure;
    while (root instanceof CompletionException && root.getCause() != null) {
      root = root.getCause();
    }
    while (root.getCause() != null) {
      root = root.getCause();
    }
    return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
  }
}

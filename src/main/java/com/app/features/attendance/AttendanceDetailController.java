package com.app.features.attendance;

import com.app.api.dto.ClassSession;
import com.app.api.dto.Course;
import com.app.components.StudentAttendanceCard;
import com.app.models.StudentAttendanceModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Handles attendance interactions and delegates network work off the UI thread. */
public final class AttendanceDetailController {

  private enum Filter {
    ALL,
    PRESENT,
    ABSENT,
    EXEMPT
  }

  private record Draft(LocalDate date, Map<String, boolean[]> marks, String observation) {}

  private record DraftSnapshot(Map<String, boolean[]> marks, String observation) {}

  private static final Map<String, Draft> DRAFTS = new ConcurrentHashMap<>();

  @FXML private Label courseCode;
  @FXML private Label courseName;
  @FXML private Label sessionDetails;
  @FXML private Label sessionState;
  @FXML private Label totalValue;
  @FXML private Label presentValue;
  @FXML private Label absentValue;
  @FXML private Label exemptValue;
  @FXML private TextField searchField;
  @FXML private VBox studentList;
  @FXML private TextArea observation;
  @FXML private Label status;
  @FXML private Button filterAllButton;
  @FXML private Button filterPresentButton;
  @FXML private Button filterAbsentButton;
  @FXML private Button filterExemptButton;
  @FXML private Button saveDraftButton;
  @FXML private Button submitButton;
  @FXML private Button allPresentButton;
  @FXML private Button allAbsentButton;

  private final AttendanceDetailViewModel viewModel;
  private final ClassSession session;
  private final Runnable onBack;
  private boolean editable;
  private boolean loadFailed;
  private boolean dirty;
  private boolean saving;
  private Filter activeFilter = Filter.ALL;
  private Timeline autosave;

  public AttendanceDetailController(
      AttendanceDetailViewModel viewModel, ClassSession session, Runnable onBack) {
    this.viewModel = viewModel;
    this.session = session;
    this.onBack = onBack;
  }

  @FXML
  private void initialize() {
    editable =
        !session.date().isAfter(LocalDate.now())
            && !session.date().isBefore(LocalDate.now().minusDays(60));
    sessionDetails.setText(session.date() + " · " + session.blockHours() + " horas");
    sessionState.setText(
        editable ? "Cargando asistencia…" : "Solo lectura · fuera del período de registro");
    status.setText("Cargando nómina y asistencia…");
    setEditingDisabled(true);
    searchField
        .textProperty()
        .addListener((observable, oldValue, newValue) -> refreshStudentList());
    observation
        .textProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (editable && !loadFailed && !saving) {
                dirty = true;
              }
            });

    Draft draft = DRAFTS.get(session.id());
    Map<String, boolean[]> draftMarks =
        draft != null && draft.date().equals(LocalDate.now()) ? draft.marks() : Map.of();
    if (draft != null && !draft.date().equals(LocalDate.now())) {
      DRAFTS.remove(session.id(), draft);
    }

    viewModel.load(
        session,
        draftMarks,
        draft != null && draft.date().equals(LocalDate.now()) ? draft.observation() : null,
        state ->
            Platform.runLater(
                () -> {
                  if (state.status() == AttendanceDetailViewModel.Status.ERROR) {
                    loadFailed = true;
                    editable = false;
                    sessionState.setText("Error de carga · solo lectura");
                    status.setText(
                        "No se pudieron cargar los datos de asistencia: " + state.message());
                    setEditingDisabled(true);
                    return;
                  }
                  if (state.status() != AttendanceDetailViewModel.Status.CONTENT) {
                    return;
                  }
                  Course course = state.course();
                  courseCode.setText(course.code() + " • SECCIÓN " + course.section());
                  courseName.setText(course.name());
                  sessionDetails.setText(
                      session.date()
                          + " · "
                          + session.blockHours()
                          + " horas · "
                          + course.schedule());
                  sessionState.setText(
                      editable
                          ? "Registro de asistencia"
                          : "Solo lectura · fuera del período de registro");
                  observation.setText(state.observation());
                  dirty = false;
                  setEditingDisabled(!editable);
                  refreshStudentList();
                  updateSummary();
                  if (draft != null && draft.date().equals(LocalDate.now())) {
                    status.setText("Borrador de hoy recuperado.");
                  } else {
                    status.setText(" ");
                  }
                  startAutosave();
                }));
    updateFilterStyles();
  }

  private void setEditingDisabled(boolean disabled) {
    searchField.setDisable(false);
    observation.setEditable(!disabled);
    saveDraftButton.setDisable(disabled);
    submitButton.setDisable(disabled);
    allPresentButton.setDisable(disabled);
    allAbsentButton.setDisable(disabled);
    filterAllButton.setDisable(false);
    filterPresentButton.setDisable(false);
    filterAbsentButton.setDisable(false);
    filterExemptButton.setDisable(false);
  }

  @FXML
  private void goBack() {
    if (onBack != null) {
      onBack.run();
    }
  }

  public void onHidden() {
    if (dirty && editable) {
      saveDraft();
    }
    if (autosave != null) {
      autosave.stop();
    }
  }

  @FXML
  private void filterAll() {
    setFilter(Filter.ALL);
  }

  @FXML
  private void filterPresent() {
    setFilter(Filter.PRESENT);
  }

  @FXML
  private void filterAbsent() {
    setFilter(Filter.ABSENT);
  }

  @FXML
  private void filterExempt() {
    setFilter(Filter.EXEMPT);
  }

  private void setFilter(Filter filter) {
    activeFilter = filter;
    updateFilterStyles();
    refreshStudentList();
  }

  private void updateFilterStyles() {
    setFilterStyle(filterAllButton, Filter.ALL);
    setFilterStyle(filterPresentButton, Filter.PRESENT);
    setFilterStyle(filterAbsentButton, Filter.ABSENT);
    setFilterStyle(filterExemptButton, Filter.EXEMPT);
  }

  private void setFilterStyle(Button button, Filter filter) {
    button.getStyleClass().removeAll("filter-pill-active");
    button.getStyleClass().removeAll("filter-pill");
    button.getStyleClass().add(filter == activeFilter ? "filter-pill-active" : "filter-pill");
  }

  @FXML
  private void markAllPresent() {
    setAllMarks(true);
  }

  @FXML
  private void markAllAbsent() {
    setAllMarks(false);
  }

  private void setAllMarks(boolean checked) {
    viewModel.students().stream()
        .filter(student -> !student.isExempt())
        .forEach(student -> student.setAllChecked(checked));
    dirty = true;
    refreshStudentList();
    updateSummary();
  }

  private void refreshStudentList() {
    if (studentList == null) {
      return;
    }
    String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
    studentList
        .getChildren()
        .setAll(
            viewModel.students().stream()
                .filter(student -> matchesFilter(student) && matchesSearch(student, query))
                .map(
                    student ->
                        new StudentAttendanceCard(
                            student,
                            editable && !saving,
                            () -> {
                              dirty = true;
                              updateSummary();
                            }))
                .toList());
  }

  private boolean matchesFilter(StudentAttendanceModel student) {
    return switch (activeFilter) {
      case ALL -> true;
      case PRESENT -> !student.isExempt() && student.isPresent();
      case ABSENT -> !student.isExempt() && student.isAbsent();
      case EXEMPT -> student.isExempt();
    };
  }

  private static boolean matchesSearch(StudentAttendanceModel student, String query) {
    return query.isBlank()
        || student.name().toLowerCase().contains(query)
        || student.code().toLowerCase().contains(query);
  }

  private void updateSummary() {
    totalValue.setText(String.valueOf(viewModel.students().size()));
    presentValue.setText(
        String.valueOf(
            viewModel.students().stream()
                .filter(student -> !student.isExempt() && student.isPresent())
                .count()));
    absentValue.setText(
        String.valueOf(
            viewModel.students().stream()
                .filter(student -> !student.isExempt() && student.isAbsent())
                .count()));
    exemptValue.setText(
        String.valueOf(
            viewModel.students().stream().filter(StudentAttendanceModel::isExempt).count()));
  }

  private void startAutosave() {
    if (!editable || loadFailed) {
      return;
    }
    autosave =
        new Timeline(
            new KeyFrame(
                Duration.seconds(60),
                event -> {
                  if (dirty) {
                    saveDraft();
                  }
                }));
    autosave.setCycleCount(Timeline.INDEFINITE);
    autosave.play();
  }

  @FXML
  private void saveDraft() {
    if (!editable || loadFailed || viewModel.students().isEmpty()) {
      return;
    }
    DraftSnapshot snapshot = snapshot();
    DRAFTS.put(session.id(), new Draft(LocalDate.now(), snapshot.marks(), snapshot.observation()));
    dirty = false;
    status.setText(
        "Borrador guardado "
            + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            + ".");
  }

  @FXML
  private void submitAttendance() {
    if (!editable || loadFailed || saving) {
      status.setText("Esta sesión no se puede editar.");
      return;
    }
    DraftSnapshot snapshot = snapshot();
    saving = true;
    setEditingDisabled(true);
    status.setText("Guardando asistencia…");
    viewModel.save(
        session,
        viewModel.course(),
        snapshot.marks(),
        snapshot.observation(),
        result ->
            Platform.runLater(
                () -> {
                  saving = false;
                  setEditingDisabled(!editable || loadFailed);
                  if (!result.successful()) {
                    status.setText("No se pudo registrar la asistencia: " + result.error());
                    refreshStudentList();
                    return;
                  }
                  DRAFTS.remove(session.id());
                  if (autosave != null) {
                    autosave.stop();
                  }
                  dirty = false;
                  sessionState.setText("Sesión registrada · solo lectura");
                  editable = false;
                  setEditingDisabled(true);
                  status.setText(
                      "Asistencia registrada "
                          + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                          + ". Sesión actualizada a DICTADA.");
                  refreshStudentList();
                  updateSummary();
                }));
  }

  private DraftSnapshot snapshot() {
    Map<String, boolean[]> marks = new HashMap<>();
    viewModel.students().stream()
        .filter(student -> !student.isExempt())
        .forEach(student -> marks.put(student.id(), student.attendanceMarks()));
    return new DraftSnapshot(marks, observation.getText());
  }
}

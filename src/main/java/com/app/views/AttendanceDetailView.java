package com.app.views;

import com.app.api.ApiClient;
import com.app.api.ApiException;
import com.app.api.dto.ClassSession;
import com.app.api.dto.Course;
import com.app.attendance.StudentAttendanceModels;
import com.app.components.StudentAttendanceCard;
import com.app.models.StudentAttendanceModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/** Real course/session attendance view used by the teacher detail flow. */
public class AttendanceDetailView extends VBox {

  private enum Filter {
    ALL, PRESENT, ABSENT, EXEMPT
  }

  private record Draft(LocalDate date, Map<String, boolean[]> marks, String observation) {
  }

  private static final Map<String, Draft> DRAFTS = new ConcurrentHashMap<>();

  private final ApiClient api;
  private final ClassSession session;
  private final Course course;
  private final List<StudentAttendanceModel> students;
  private final VBox studentList = new VBox(10);
  private HBox filterBar;
  private final TextField searchField = new TextField();
  private final Label totalValue = new Label("0");
  private final Label presentValue = new Label("0");
  private final Label absentValue = new Label("0");
  private final Label exemptValue = new Label("0");
  private final Label status = new Label();
  private final TextArea observation = new TextArea();
  private final boolean editable;
  private Filter activeFilter = Filter.ALL;
  private Timeline autosave;
  private boolean dirty;

  public AttendanceDetailView(ApiClient api, ClassSession session, Runnable onBack) {
    this.api = api;
    this.session = session;
    this.course = findCourse(api, session.courseId());
    this.editable = !session.date().isAfter(LocalDate.now());

    Draft draft = DRAFTS.get(session.id());
    Map<String, boolean[]> draftMarks = draft != null && draft.date().equals(LocalDate.now())
        ? draft.marks()
        : Map.of();
    if (draftMarks.isEmpty() && draft != null && !draft.date().equals(LocalDate.now())) {
      DRAFTS.remove(session.id(), draft);
    }
    List<StudentAttendanceModel> loadedStudents;
    try {
      loadedStudents = StudentAttendanceModels.load(
          api, course, session.blockHours(), safeSavedMarks(), draftMarks);
    } catch (ApiException exception) {
      loadedStudents = List.of();
      status.setText("No se pudo cargar la nómina: " + exception.getMessage());
    }
    this.students = loadedStudents;
    if (draft != null && draft.date().equals(LocalDate.now())) {
      observation.setText(draft.observation());
    }

    setSpacing(14);
    setPadding(new Insets(16));
    setStyle("-fx-background-color: #F8FAFC;");

    getChildren().addAll(
        createSessionHeader(onBack),
        createSummarySection(),
        createSearchBar(),
        createFilters(),
        createMassActions(),
        studentList,
        createObservationSection(),
        createFooter());

    searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshStudentList());
    observation.textProperty().addListener((observable, oldValue, newValue) -> dirty = true);
    refreshStudentList();
    updateSummary();
    if (draft != null && draft.date().equals(LocalDate.now())) {
      status.setText("Borrador de hoy recuperado.");
    }
    startAutosave();
  }

  private static Course findCourse(ApiClient api, String courseId) {
    try {
      return api.coursesForCurrentUser().stream()
          .filter(item -> item.id().equals(courseId))
          .findFirst()
          .orElseGet(() -> new Course(courseId, courseId, courseId, 0));
    } catch (ApiException exception) {
      return new Course(courseId, courseId, courseId, 0);
    }
  }

  private Map<String, boolean[]> safeSavedMarks() {
    try {
      return api.attendanceForSession(session.id());
    } catch (ApiException exception) {
      status.setText("No se pudieron cargar las marcas guardadas: " + exception.getMessage());
      return Map.of();
    }
  }

  private VBox createSessionHeader(Runnable onBack) {
    VBox box = new VBox(8);
    box.setPadding(new Insets(14));
    box.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; "
        + "-fx-border-color: #E2E8F0; -fx-border-radius: 12px;");

    Button back = new Button("← Mis cursos");
    back.setOnAction(event -> {
      if (dirty && editable)
        saveDraft();
      if (autosave != null)
        autosave.stop();
      if (onBack != null)
        onBack.run();
    });
    Label courseCode = new Label(course.code() + " • SECCIÓN " + course.section());
    courseCode.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
    Label title = new Label(course.name());
    title.setWrapText(true);
    title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
    Label details = new Label(session.date() + " · " + session.blockHours() + " horas · " + course.schedule());
    details.setWrapText(true);
    details.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
    Label sessionState = new Label(editable ? "Registro de asistencia" : "Sesión futura · solo lectura");
    sessionState.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
    box.getChildren().addAll(back, courseCode, title, details, sessionState);
    return box;
  }

  private VBox createSummarySection() {
    VBox section = new VBox(8);
    Label heading = new Label("NÓMINA Y ASISTENCIA");
    heading.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
    HBox stats = new HBox(8,
        createStatCard(totalValue, "Total", "#F1F5F9", "#0F172A"),
        createStatCard(presentValue, "Presentes", "#DCFCE7", "#166534"),
        createStatCard(absentValue, "Faltas", "#FEE2E2", "#991B1B"),
        createStatCard(exemptValue, "Exentos", "#DBEAFE", "#1E40AF"));
    section.getChildren().addAll(heading, stats);
    return section;
  }

  private static VBox createStatCard(Label value, String label, String background, String foreground) {
    VBox card = new VBox(2, value, new Label(label));
    card.setAlignment(Pos.CENTER);
    card.setPadding(new Insets(8));
    card.setMaxWidth(Double.MAX_VALUE);
    card.setStyle("-fx-background-color: " + background + "; -fx-background-radius: 10px; "
        + "-fx-text-fill: " + foreground + ";");
    value.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + foreground + ";");
    card.getChildren().get(1).setStyle("-fx-font-size: 10px; -fx-text-fill: " + foreground + ";");
    HBox.setHgrow(card, Priority.ALWAYS);
    return card;
  }

  private HBox createSearchBar() {
    HBox search = new HBox(8, new FontIcon(Feather.SEARCH), searchField);
    search.setAlignment(Pos.CENTER_LEFT);
    search.setPadding(new Insets(8, 12, 8, 12));
    search.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; "
        + "-fx-border-color: #E2E8F0; -fx-border-radius: 10px;");
    searchField.setPromptText("Buscar por nombre o código de alumno...");
    searchField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
    HBox.setHgrow(searchField, Priority.ALWAYS);
    return search;
  }

  private HBox createFilters() {
    filterBar = new HBox(6);
    filterBar.getChildren().addAll(
        filterButton("Todos", Filter.ALL),
        filterButton("Presentes", Filter.PRESENT),
        filterButton("Faltas", Filter.ABSENT),
        filterButton("Exentos", Filter.EXEMPT));
    return filterBar;
  }

  private Button filterButton(String label, Filter filter) {
    Button button = new Button(label);
    button.setOnAction(event -> {
      activeFilter = filter;
      filterBar.getChildren().forEach(node -> {
        Button pill = (Button) node;
        pill.setStyle(pill == button ? activePillStyle() : inactivePillStyle());
      });
      refreshStudentList();
    });
    button.setStyle(filter == activeFilter ? activePillStyle() : inactivePillStyle());
    return button;
  }

  private HBox createMassActions() {
    HBox actions = new HBox(8);
    actions.setAlignment(Pos.CENTER_LEFT);
    Label label = new Label("Acción masiva");
    label.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B; -fx-font-weight: bold;");
    Button allPresent = new Button("Marcar todos presentes");
    allPresent.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #166534; -fx-font-weight: bold; "
        + "-fx-font-size: 11px; -fx-background-radius: 8px;");
    allPresent.setOnAction(event -> setAllMarks(true));
    Button allAbsent = new Button("Marcar todos ausentes");
    allAbsent.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-weight: bold; "
        + "-fx-font-size: 11px; -fx-background-radius: 8px;");
    allAbsent.setOnAction(event -> setAllMarks(false));
    allPresent.setDisable(!editable);
    allAbsent.setDisable(!editable);
    HBox.setHgrow(allPresent, Priority.ALWAYS);
    HBox.setHgrow(allAbsent, Priority.ALWAYS);
    allPresent.setMaxWidth(Double.MAX_VALUE);
    allAbsent.setMaxWidth(Double.MAX_VALUE);
    actions.getChildren().addAll(label, allPresent, allAbsent);
    return actions;
  }

  private Accordion createObservationSection() {
    Accordion accordion = new Accordion();
    observation.setPromptText("Observaciones de esta sesión");
    observation.setPrefRowCount(3);
    TitledPane pane = new TitledPane("Observaciones generales de la sesión", observation);
    accordion.getPanes().add(pane);
    return accordion;
  }

  private VBox createFooter() {
    VBox footer = new VBox(8);
    HBox buttons = new HBox(10);
    Button saveDraft = new Button("Guardar borrador");
    saveDraft.setDisable(!editable);
    saveDraft.setOnAction(event -> saveDraft());
    saveDraft.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(saveDraft, Priority.ALWAYS);

    Button submit = new Button("Registrar sesión");
    submit.setStyle("-fx-background-color: #064E3B; -fx-text-fill: white; -fx-font-weight: bold; "
        + "-fx-background-radius: 10px;");
    submit.setDisable(!editable);
    submit.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(submit, Priority.ALWAYS);
    submit.setOnAction(event -> submitAttendance());
    buttons.getChildren().addAll(saveDraft, submit);
    status.setWrapText(true);
    footer.getChildren().addAll(buttons, status);
    return footer;
  }

  private void refreshStudentList() {
    String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
    studentList.getChildren().clear();
    students.stream()
        .filter(student -> matchesFilter(student) && matchesSearch(student, query))
        .map(student -> new StudentAttendanceCard(student, editable, () -> {
          dirty = true;
          updateSummary();
        }))
        .forEach(studentList.getChildren()::add);
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
    return query.isBlank() || student.name().toLowerCase().contains(query)
        || student.code().toLowerCase().contains(query);
  }

  private void setAllMarks(boolean checked) {
    students.stream().filter(student -> !student.isExempt()).forEach(student -> student.setAllChecked(checked));
    dirty = true;
    refreshStudentList();
    updateSummary();
  }

  private void updateSummary() {
    totalValue.setText(String.valueOf(students.size()));
    presentValue.setText(String.valueOf(students.stream()
        .filter(student -> !student.isExempt() && student.isPresent()).count()));
    absentValue.setText(String.valueOf(students.stream()
        .filter(student -> !student.isExempt() && student.isAbsent()).count()));
    exemptValue.setText(String.valueOf(students.stream().filter(StudentAttendanceModel::isExempt).count()));
  }

  private void startAutosave() {
    if (!editable)
      return;
    autosave = new Timeline(new KeyFrame(Duration.seconds(60), event -> {
      if (dirty)
        saveDraft();
    }));
    autosave.setCycleCount(Timeline.INDEFINITE);
    autosave.play();
  }

  private void saveDraft() {
    DRAFTS.put(session.id(), new Draft(LocalDate.now(), snapshot(), observation.getText()));
    dirty = false;
    status.setText("Borrador guardado "
        + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + ".");
  }

  private void submitAttendance() {
    if (session.date().isAfter(LocalDate.now())) {
      new Alert(Alert.AlertType.ERROR, "No se permite registrar asistencia en fechas futuras.").showAndWait();
      return;
    }
    try {
      api.saveAttendance(session.id(), snapshot());
      DRAFTS.remove(session.id());
      if (autosave != null)
        autosave.stop();
      status.setText("Asistencia registrada " + LocalDateTime.now()
          .format(DateTimeFormatter.ofPattern("HH:mm:ss")) + ". Sesión actualizada a DICTADA.");
    } catch (ApiException exception) {
      new Alert(Alert.AlertType.ERROR, exception.getMessage()).showAndWait();
    }
  }

  private Map<String, boolean[]> snapshot() {
    Map<String, boolean[]> marks = new HashMap<>();
    students.forEach(student -> marks.put(student.id(), student.attendanceMarks()));
    return marks;
  }

  private static String activePillStyle() {
    return "-fx-background-color: #064E3B; -fx-text-fill: white; -fx-background-radius: 20px; "
        + "-fx-font-weight: bold; -fx-font-size: 11px;";
  }

  private static String inactivePillStyle() {
    return "-fx-background-color: #FFFFFF; -fx-text-fill: #475569; -fx-background-radius: 20px; "
        + "-fx-border-color: #E2E8F0; -fx-border-radius: 20px; -fx-font-size: 11px;";
  }
}

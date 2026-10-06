package com.app.api;

import com.app.api.dto.ClassSession;
import com.app.api.dto.ClassSession.SessionStatus;
import com.app.api.dto.Course;
import com.app.api.dto.LoginRequest;
import com.app.api.dto.SessionTokens;
import com.app.api.dto.Student;
import com.app.api.dto.StudentSummary;
import com.app.common.BrCalculations;
import com.app.common.RiskLevel;
import com.app.config.AppConfig;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local mock (US-01/US-08/US-15/US-16). Demo users: {@code admin|docente|alumno / demo}. Generates
 * HS256 JWT only for client-side claim inspection (the backend is the authority).
 */
public class MockApiClient implements ApiClient {

  private record MockUser(String username, String displayName, Set<String> groups) {}

  private static final Map<String, MockUser> USERS =
      Map.of(
          "admin", new MockUser("admin", "Ada Admin", Set.of("ADMIN")),
          "docente", new MockUser("docente", "Dan Docente", Set.of("DOCENTE")),
          "alumno", new MockUser("alumno", "Al Alumno", Set.of("ALUMNO")));

  private static final List<Course> COURSES =
      List.of(
          new Course(
              "mat-001",
              "MAT-101",
              "Matemática I",
              64,
              "SEC-01A",
              "Dan Docente",
              "Lun–Mié 08:00–10:00"),
          new Course(
              "mat-002",
              "FIS-102",
              "Física II",
              48,
              "SEC-02B",
              "Ada Admin",
              "Mar–Jue 10:00–12:00"));

  private final Map<String, ClassSession> sessions = new ConcurrentHashMap<>();
  // studentId -> (sessionId -> attended hours)
  private final Map<String, Map<String, Integer>> attended = new ConcurrentHashMap<>();
  private final Map<String, Map<String, boolean[]>> attendanceBySession = new ConcurrentHashMap<>();
  private final Map<String, String> observationsBySession = new ConcurrentHashMap<>();
  // studentId -> (courseId -> excused / late-enrollment exempt hours)
  private final Map<String, Map<String, Integer>> excused = new ConcurrentHashMap<>();
  private final Map<String, Map<String, Integer>> exempt = new ConcurrentHashMap<>();
  private final List<String> students = List.of("alu-01", "alu-02", "alu-03");

  private static final List<Student> ROSTER =
      List.of(
          new Student("alu-01", "2024-001", "Ana Quispe Mamani"),
          new Student("alu-02", "2024-002", "Bruno Paredes Ríos"),
          new Student("alu-03", "2024-003", "Carla Flores Vega"));

  private volatile String currentUser = "docente";

  public MockApiClient() {
    LocalDate today = LocalDate.now();
    // 3 valid past sessions + 1 future (must be filtered) + 1 holiday (must be filtered)
    save(new ClassSession("ses-01", "mat-001", today.minusDays(14), 2, SessionStatus.DICTADA));
    save(new ClassSession("ses-02", "mat-001", today.minusDays(7), 2, SessionStatus.PROGRAMADA));
    save(new ClassSession("ses-03", "mat-001", today.minusDays(1), 3, SessionStatus.PROGRAMADA));
    save(new ClassSession("ses-fut", "mat-001", today.plusDays(7), 2, SessionStatus.PROGRAMADA));
    save(new ClassSession("ses-fer", "mat-001", today.minusDays(3), 2, SessionStatus.FERIADA));
    // My-courses window (today … today+5, both courses). ses-fut (+7) stays out on purpose.
    save(new ClassSession("ses-hoy-001", "mat-001", today, 2, SessionStatus.PROGRAMADA));
    save(new ClassSession("ses-d1-002", "mat-002", today.plusDays(1), 2, SessionStatus.PROGRAMADA));
    save(new ClassSession("ses-d2-001", "mat-001", today.plusDays(2), 3, SessionStatus.PROGRAMADA));
    save(new ClassSession("ses-d3-002", "mat-002", today.plusDays(3), 2, SessionStatus.PROGRAMADA));
    save(new ClassSession("ses-d4-001", "mat-001", today.plusDays(4), 2, SessionStatus.PROGRAMADA));
    save(new ClassSession("ses-d5-002", "mat-002", today.plusDays(5), 3, SessionStatus.PROGRAMADA));
    for (String s : students) {
      attended.put(s, new ConcurrentHashMap<>(Map.of("ses-01", 2)));
      excused.put(s, new ConcurrentHashMap<>());
      exempt.put(s, new ConcurrentHashMap<>());
    }
    // Fixture BR-02/BR-04: alu-02 with 2 excused hours in mat-001 (neither margin nor denominator)
    excused.get("alu-02").put("mat-001", 2);
  }

  private void save(ClassSession s) {
    sessions.put(s.id(), s);
  }

  private static String fakeJwt(String username, Set<String> groups) {
    return JWT.create()
        .withSubject(username)
        .withArrayClaim("groups", groups.toArray(String[]::new))
        .withExpiresAt(Date.from(Instant.now().plus(Duration.ofHours(8))))
        .sign(Algorithm.HMAC256("mock-secret-solo-demo"));
  }

  @Override
  public SessionTokens login(LoginRequest req) throws ApiException {
    // Generic error (US-01 CA5): never reveal whether the user exists.
    MockUser u = USERS.get(req.username().toLowerCase());
    if (u == null || !"demo".equals(req.password())) {
      throw new ApiException(ApiException.Kind.AUTH, "Usuario o contraseña inválidos");
    }
    currentUser = u.username();
    return new SessionTokens(
        fakeJwt(u.username(), u.groups()),
        "refresh-mock-" + u.username(),
        u.username(),
        u.displayName(),
        u.groups(),
        Instant.now().plus(Duration.ofHours(8)));
  }

  @Override
  public SessionTokens refresh(String refreshToken) throws ApiException {
    // Mock format: refresh-mock-<username>. Generic 401 on mismatch (mvp.md §2.1).
    if (refreshToken == null || !refreshToken.startsWith("refresh-mock-")) {
      throw new ApiException(ApiException.Kind.AUTH, "Sesión expirada");
    }
    String username = refreshToken.substring("refresh-mock-".length()).toLowerCase();
    MockUser u = USERS.get(username);
    if (u == null) {
      throw new ApiException(ApiException.Kind.AUTH, "Sesión expirada");
    }
    currentUser = u.username();
    return new SessionTokens(
        fakeJwt(u.username(), u.groups()),
        "refresh-mock-" + u.username(),
        u.username(),
        u.displayName(),
        u.groups(),
        Instant.now().plus(Duration.ofHours(8)));
  }

  @Override
  public List<Course> coursesForCurrentUser() {
    return new ArrayList<>(COURSES);
  }

  @Override
  public List<ClassSession> validSessions(String courseId) {
    LocalDate today = LocalDate.now();
    return sessions.values().stream()
        .filter(s -> s.courseId().equals(courseId))
        .filter(s -> !s.date().isAfter(today)) // US-08 CA5: no future dates
        .filter(s -> s.status() == SessionStatus.PROGRAMADA || s.status() == SessionStatus.DICTADA)
        .sorted((a, b) -> a.date().compareTo(b.date()))
        .toList();
  }

  @Override
  public List<Student> studentsByCourse(String courseId) {
    return List.copyOf(ROSTER);
  }

  @Override
  public List<ClassSession> sessionsWindow(LocalDate from, LocalDate to) {
    return sessions.values().stream()
        .filter(s -> !s.date().isBefore(from) && !s.date().isAfter(to))
        .filter(s -> s.status() == SessionStatus.PROGRAMADA || s.status() == SessionStatus.DICTADA)
        .sorted((a, b) -> a.date().compareTo(b.date()))
        .toList();
  }

  @Override
  public StudentSummary studentSummary(String courseId, String studentId) {
    List<ClassSession> delivered =
        sessions.values().stream()
            .filter(s -> s.courseId().equals(courseId) && s.status() == SessionStatus.DICTADA)
            .toList();
    double taught = delivered.stream().mapToDouble(ClassSession::blockHours).sum();
    int present =
        attended.getOrDefault(studentId, Map.of()).entrySet().stream()
            .filter(
                e -> {
                  ClassSession s = sessions.get(e.getKey());
                  return s != null
                      && s.courseId().equals(courseId)
                      && s.status() == SessionStatus.DICTADA;
                })
            .mapToInt(Map.Entry::getValue)
            .sum();
    // BR-02/BR-04: excused and exempt hours consume neither margin nor denominator (BR-03/BR-06)
    double exc = excused.getOrDefault(studentId, Map.of()).getOrDefault(courseId, 0);
    double exe = exempt.getOrDefault(studentId, Map.of()).getOrDefault(courseId, 0);
    double missed = Math.max(0, taught - present - exc - exe);
    double pct = BrCalculations.attendancePercentage(present, taught, exc, exe);
    double required =
        COURSES.stream()
            .filter(c -> c.id().equals(courseId))
            .mapToDouble(Course::totalHours)
            .findFirst()
            .orElse(64);
    double margin = BrCalculations.marginHours(required, AppConfig.maxFaltasRatio());
    RiskLevel risk = BrCalculations.risk(missed, margin);
    return new StudentSummary(
        courseId,
        studentId,
        taught,
        present,
        missed,
        exc,
        exe,
        pct,
        margin,
        BrCalculations.remainingHours(missed, margin),
        risk);
  }

  @Override
  public Map<String, boolean[]> attendanceForSession(String sessionId) {
    Map<String, boolean[]> saved = attendanceBySession.getOrDefault(sessionId, Map.of());
    Map<String, boolean[]> copy = new HashMap<>();
    saved.forEach((studentId, marks) -> copy.put(studentId, marks.clone()));
    return copy;
  }

  @Override
  public String attendanceObservation(String sessionId) {
    return observationsBySession.getOrDefault(sessionId, "");
  }

  /** Fixtures/tests only: sets excused/exempt hours per student+course. */
  void setExcused(String studentId, String courseId, int hours) {
    excused.computeIfAbsent(studentId, k -> new ConcurrentHashMap<>()).put(courseId, hours);
  }

  void setExempt(String studentId, String courseId, int hours) {
    exempt.computeIfAbsent(studentId, k -> new ConcurrentHashMap<>()).put(courseId, hours);
  }

  List<String> internalStudents() {
    return List.copyOf(students);
  }

  @Override
  public void saveAttendance(
      String sessionId, Map<String, boolean[]> attendanceByStudent, String observation) {
    ClassSession s = sessions.get(sessionId);
    if (s == null) {
      throw new ApiException(ApiException.Kind.NOT_FOUND, "Sesión no encontrada");
    }
    if (s.date().isAfter(LocalDate.now())) {
      throw new ApiException(
          ApiException.Kind.VALIDATION, "No se permite tomar asistencia en fechas futuras");
    }
    if (s.status() == SessionStatus.FERIADA || s.status() == SessionStatus.SUSPENDIDA) {
      throw new ApiException(ApiException.Kind.VALIDATION, "Sesión feriada/suspendida");
    }
    for (var e : attendanceByStudent.entrySet()) {
      boolean[] arr = e.getValue();
      if (arr == null || arr.length != s.blockHours()) {
        throw new ApiException(
            ApiException.Kind.VALIDATION, "Se esperaban " + s.blockHours() + " marcas por alumno");
      }
      int count = 0;
      for (boolean b : arr) {
        if (b) {
          count++;
        }
      }
      attended.computeIfAbsent(e.getKey(), k -> new ConcurrentHashMap<>()).put(sessionId, count);
    }
    Map<String, boolean[]> copiedMarks = new HashMap<>();
    attendanceByStudent.forEach((studentId, marks) -> copiedMarks.put(studentId, marks.clone()));
    attendanceBySession.put(sessionId, copiedMarks);
    observationsBySession.put(sessionId, observation == null ? "" : observation);
    // US-08 CA4: saving moves the session to DICTADA
    sessions.put(
        sessionId,
        new ClassSession(s.id(), s.courseId(), s.date(), s.blockHours(), SessionStatus.DICTADA));
  }

  // Tests only
  Map<String, ClassSession> internalSessions() {
    return new HashMap<>(sessions);
  }
}

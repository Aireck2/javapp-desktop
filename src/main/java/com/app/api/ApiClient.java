package com.app.api;

import com.app.api.dto.ClassSession;
import com.app.api.dto.Course;
import com.app.api.dto.LoginRequest;
import com.app.api.dto.SessionTokens;
import com.app.api.dto.Student;
import com.app.api.dto.StudentSummary;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * HTTP contract (MVP phase: {@link MockApiClient}). Phase 2: real
 * {@code HttpApiClient}.
 */
public interface ApiClient {

  SessionTokens login(LoginRequest req) throws ApiException;

  /**
   * Renews the session with the stored refresh token (mvp.md §2.1 Splash).
   * Equivalent to {@code POST /api/v1/auth/refresh}. Throws {@code AUTH}
   * (401) when invalid/expired.
   */
  SessionTokens refresh(String refreshToken) throws ApiException;

  List<Course> coursesForCurrentUser() throws ApiException;

  /**
   * Only valid dates US-08: past/today, PROGRAMADA or DICTADA (never
   * FERIADA/SUSPENDIDA/future).
   */
  List<ClassSession> validSessions(String courseId) throws ApiException;

  /** Course roster (detail view): students enrolled in the course. */
  List<Student> studentsByCourse(String courseId) throws ApiException;

  /**
   * Session window for "My courses" (today … today+5): includes future dates
   * inside the range, excludes FERIADA/SUSPENDIDA, ordered by date.
   */
  List<ClassSession> sessionsWindow(LocalDate from, LocalDate to) throws ApiException;

  StudentSummary studentSummary(String courseId, String studentId) throws ApiException;

  /** Returns saved per-hour marks for a session, when it has already been recorded. */
  Map<String, boolean[]> attendanceForSession(String sessionId) throws ApiException;

  /** Returns the general observation saved with a session's attendance. */
  String attendanceObservation(String sessionId) throws ApiException;

  /**
   * Saves attendance. {@code attendanceByStudent}: studentId → N-boolean array
   * (one per teaching hour of the block; true = present).
   * On save the session moves to DICTADA (US-08 CA4).
   */
  default void saveAttendance(String sessionId, Map<String, boolean[]> attendanceByStudent) throws ApiException {
    saveAttendance(sessionId, attendanceByStudent, "");
  }

  /** Saves attendance marks and the session observation in one operation. */
  void saveAttendance(
      String sessionId,
      Map<String, boolean[]> attendanceByStudent,
      String observation) throws ApiException;
}

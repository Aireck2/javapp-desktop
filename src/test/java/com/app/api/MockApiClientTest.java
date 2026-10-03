package com.app.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.auth0.jwt.JWT;
import com.app.api.dto.ClassSession;
import com.app.api.dto.ClassSession.SessionStatus;
import com.app.api.dto.LoginRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MockApiClientTest {

    @Test
    void login_ok_exposesGroupsInJwt() {
        var api = new MockApiClient();
        var tok = api.login(new LoginRequest("docente", "demo"));
        assertThat(tok.groups()).contains("DOCENTE");
        var decoded = JWT.decode(tok.accessToken());
        assertThat(List.of(decoded.getClaim("groups").asArray(String.class))).contains("DOCENTE");
    }

    @Test
    void login_bad_genericMessage() {
        var api = new MockApiClient();
        assertThatThrownBy(() -> api.login(new LoginRequest("nobody", "x")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Usuario o contraseña inválidos");
        assertThatThrownBy(() -> api.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Usuario o contraseña inválidos");
    }

    @Test
    void validSessions_filtersFutureAndHoliday() {
        var api = new MockApiClient();
        List<ClassSession> valid = api.validSessions("mat-001");
        assertThat(valid).isNotEmpty();
        assertThat(valid).allMatch(s ->
                !s.date().isAfter(java.time.LocalDate.now())
                        && (s.status() == SessionStatus.PROGRAMADA
                                || s.status() == SessionStatus.DICTADA));
        assertThat(valid.stream().map(ClassSession::id)).doesNotContain("ses-fut", "ses-fer");
    }

    @Test
    void save_marksDelivered_and_summaryRecalculates() {
        var api = new MockApiClient();
        var before = api.internalSessions().get("ses-02");
        assertThat(before.status()).isEqualTo(SessionStatus.PROGRAMADA);
        api.saveAttendance("ses-02", Map.of("alu-01", new boolean[] {true, false}));
        assertThat(api.internalSessions().get("ses-02").status())
                .isEqualTo(SessionStatus.DICTADA);
        var summary = api.studentSummary("mat-001", "alu-01");
        assertThat(summary.taughtHours()).isGreaterThan(0);
        assertThat(summary.percentage()).isBetween(0.0, 100.0);
    }

    @Test
    void save_future_throwsValidation() {
        var api = new MockApiClient();
        assertThatThrownBy(() -> api.saveAttendance("ses-fut", Map.of("alu-01", new boolean[] {true, true})))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void refresh_ok_renewsJwt() {
        var api = new MockApiClient();
        var login = api.login(new LoginRequest("alumno", "demo"));
        var renewed = api.refresh(login.refreshToken());
        assertThat(renewed.groups()).contains("ALUMNO");
        assertThat(renewed.accessToken()).isNotBlank();
    }

    @Test
    void refresh_invalid_401() {
        var api = new MockApiClient();
        assertThatThrownBy(() -> api.refresh("refresh-mock-nadie"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Sesión expirada");
        assertThatThrownBy(() -> api.refresh("token-roto"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void excused_consumesNeitherMarginNorDenominator() {
        var api = new MockApiClient();
        api.setExcused("alu-03", "mat-001", 2);
        var summary = api.studentSummary("mat-001", "alu-03");
        assertThat(summary.excusedHours()).isEqualTo(2);
        // 6/(10−2)=75% style: denominator excludes excused hours
        double expected = summary.taughtHours() - summary.excusedHours() - summary.exemptHours() <= 0 ? 100.0
                : summary.attendedHours() / (summary.taughtHours() - summary.excusedHours() - summary.exemptHours())
                        * 100.0;
        assertThat(summary.percentage()).isCloseTo(Math.min(100.0, Math.max(0.0, expected)),
                org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    void course_bringsSectionTeacherSchedule() {
        var api = new MockApiClient();
        assertThat(api.coursesForCurrentUser())
                .allMatch(c -> c.section() != null && c.teacher() != null && c.schedule() != null);
    }

    @Test
    void studentsByCourse_bringsCodeAndName() {
        var api = new MockApiClient();
        var roster = api.studentsByCourse("mat-001");
        assertThat(roster).hasSize(3);
        assertThat(roster).allMatch(s -> s.code() != null && !s.code().isBlank()
                && s.fullName() != null && !s.fullName().isBlank());
    }

    @Test
    void sessionsWindow_todayPlus5_groupsAndExcludesHoliday() {
        var api = new MockApiClient();
        var today = java.time.LocalDate.now();
        var window = api.sessionsWindow(today, today.plusDays(5));
        assertThat(window).isNotEmpty();
        assertThat(window.stream().map(ClassSession::id)).doesNotContain("ses-fer", "ses-fut");
        assertThat(window).allMatch(s -> !s.date().isBefore(today) && !s.date().isAfter(today.plusDays(5)));
        var dates = window.stream().map(ClassSession::date).toList();
        assertThat(dates).isSorted();
    }

    @Test
    void sessionsWindow_emptyRange_returnsEmpty() {
        var api = new MockApiClient();
        var today = java.time.LocalDate.now();
        assertThat(api.sessionsWindow(today.plusDays(6), today.plusDays(6))).isEmpty();
    }
}

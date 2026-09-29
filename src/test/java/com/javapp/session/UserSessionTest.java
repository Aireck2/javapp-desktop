package com.javapp.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class UserSessionTest {

    @AfterEach
    void cleanup() {
        UserSession.getInstance().logout();
    }

    @Test
    void login_exponeRoles_y_logoutLimpia() {
        var s = UserSession.getInstance();
        s.login("tok", "admin", "Admin", Set.of("ADMIN"), Instant.now().plus(Duration.ofHours(1)));
        assertThat(s.isLoggedIn()).isTrue();
        assertThat(s.hasRole("ADMIN")).isTrue();
        assertThat(s.hasRole("admin")).isTrue(); // case-insensitive
        assertThat(s.hasAnyRole("DOCENTE", "ADMIN")).isTrue();
        assertThat(s.hasRole("ALUMNO")).isFalse();
        s.logout();
        assertThat(s.isLoggedIn()).isFalse();
    }

    @Test
    void tokenExpirado_noEsLoggedIn() {
        var s = UserSession.getInstance();
        s.login("tok", "doc", "Doc", Set.of("DOCENTE"), Instant.now().minusSeconds(5));
        assertThat(s.isExpired()).isTrue();
        assertThat(s.isLoggedIn()).isFalse();
    }

    @Test
    void inactividad_superaTimeout() {
        var s = UserSession.getInstance();
        s.login("tok", "alu", "Alu", Set.of("ALUMNO"), Instant.now().plus(Duration.ofHours(1)));
        s.setLastActivityForTest(Instant.now().minus(Duration.ofMinutes(16)));
        assertThat(s.isInactive(Duration.ofMinutes(15))).isTrue();
        s.touch();
        assertThat(s.isInactive(Duration.ofMinutes(15))).isFalse();
    }
}

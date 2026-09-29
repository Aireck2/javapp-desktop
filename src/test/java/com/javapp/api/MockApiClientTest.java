package com.javapp.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.auth0.jwt.JWT;
import com.javapp.api.dto.LoginRequest;
import com.javapp.api.dto.SesionClase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MockApiClientTest {

    @Test
    void login_ok_exponeGroupsEnJwt() {
        var api = new MockApiClient();
        var tok = api.login(new LoginRequest("docente", "demo"));
        assertThat(tok.groups()).contains("DOCENTE");
        var decoded = JWT.decode(tok.accessToken());
        assertThat(List.of(decoded.getClaim("groups").asArray(String.class))).contains("DOCENTE");
    }

    @Test
    void login_malo_mensajeGenerico() {
        var api = new MockApiClient();
        assertThatThrownBy(() -> api.login(new LoginRequest("nadie", "x")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Usuario o contraseña inválidos");
        assertThatThrownBy(() -> api.login(new LoginRequest("admin", "mala")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Usuario o contraseña inválidos");
    }

    @Test
    void sesionesValidas_filtraFuturaYFeriada() {
        var api = new MockApiClient();
        List<SesionClase> validas = api.sesionesValidas("mat-001");
        assertThat(validas).isNotEmpty();
        assertThat(validas).allMatch(s ->
                !s.fecha().isAfter(java.time.LocalDate.now())
                        && (s.estado() == SesionClase.EstadoSesion.PROGRAMADA
                                || s.estado() == SesionClase.EstadoSesion.DICTADA));
        assertThat(validas.stream().map(SesionClase::id)).doesNotContain("ses-fut", "ses-fer");
    }

    @Test
    void guardar_marcaDictada_y_resumenRecalcula() {
        var api = new MockApiClient();
        var antes = api.sesionesInternas().get("ses-02");
        assertThat(antes.estado()).isEqualTo(SesionClase.EstadoSesion.PROGRAMADA);
        api.guardarAsistencia("ses-02", Map.of("alu-01", new boolean[] {true, false}));
        assertThat(api.sesionesInternas().get("ses-02").estado())
                .isEqualTo(SesionClase.EstadoSesion.DICTADA);
        var resumen = api.resumenAlumno("mat-001", "alu-01");
        assertThat(resumen.hDictadas()).isGreaterThan(0);
        assertThat(resumen.porcentaje()).isBetween(0.0, 100.0);
    }

    @Test
    void guardar_futura_lanzaValidacion() {
        var api = new MockApiClient();
        assertThatThrownBy(() -> api.guardarAsistencia("ses-fut", Map.of("alu-01", new boolean[] {true, true})))
                .isInstanceOf(ApiException.class);
    }
}

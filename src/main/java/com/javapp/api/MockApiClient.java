package com.javapp.api;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.javapp.api.dto.LoginRequest;
import com.javapp.api.dto.Materia;
import com.javapp.api.dto.SesionClase;
import com.javapp.api.dto.SesionClase.EstadoSesion;
import com.javapp.api.dto.SessionTokens;
import com.javapp.api.dto.StudentSummary;
import com.javapp.common.BrCalculations;
import com.javapp.common.RiskLevel;
import com.javapp.config.AppConfig;
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
 * Mock local (US-01/US-08/US-15/US-16). Usuarios demo: {@code admin|docente|alumno / demo}.
 * Genera JWT HS256 solo para inspección de claims en cliente (la autoridad es el backend).
 */
public class MockApiClient implements ApiClient {

    private record MockUser(String username, String displayName, Set<String> groups) {}

    private static final Map<String, MockUser> USERS =
            Map.of(
                    "admin", new MockUser("admin", "Ada Admin", Set.of("ADMIN")),
                    "docente", new MockUser("docente", "Dan Docente", Set.of("DOCENTE")),
                    "alumno", new MockUser("alumno", "Al Alumno", Set.of("ALUMNO")));

    private static final List<Materia> MATERIAS =
            List.of(
                    new Materia("mat-001", "MAT-101", "Matemática I", 64),
                    new Materia("mat-002", "FIS-102", "Física II", 48));

    private final Map<String, SesionClase> sesiones = new ConcurrentHashMap<>();
    // alumnoId -> (sesionId -> horas presentes)
    private final Map<String, Map<String, Integer>> presentes = new ConcurrentHashMap<>();
    private final List<String> alumnos = List.of("alu-01", "alu-02", "alu-03");

    private volatile String currentUser = "docente";

    public MockApiClient() {
        LocalDate hoy = LocalDate.now();
        // 3 pasadas válidas + 1 futura (debe filtrarse) + 1 feriada (debe filtrarse)
        save(new SesionClase("ses-01", "mat-001", hoy.minusDays(14), 2, EstadoSesion.DICTADA));
        save(new SesionClase("ses-02", "mat-001", hoy.minusDays(7), 2, EstadoSesion.PROGRAMADA));
        save(new SesionClase("ses-03", "mat-001", hoy.minusDays(1), 3, EstadoSesion.PROGRAMADA));
        save(new SesionClase("ses-fut", "mat-001", hoy.plusDays(7), 2, EstadoSesion.PROGRAMADA));
        save(new SesionClase("ses-fer", "mat-001", hoy.minusDays(3), 2, EstadoSesion.FERIADA));
        for (String a : alumnos) {
            presentes.put(a, new ConcurrentHashMap<>(Map.of("ses-01", 2)));
        }
    }

    private void save(SesionClase s) {
        sesiones.put(s.id(), s);
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
        // Error genérico (US-01 CA5): no revelar si el usuario existe.
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
    public List<Materia> materiasForCurrentUser() {
        return new ArrayList<>(MATERIAS);
    }

    @Override
    public List<SesionClase> sesionesValidas(String materiaId) {
        LocalDate hoy = LocalDate.now();
        return sesiones.values().stream()
                .filter(s -> s.materiaId().equals(materiaId))
                .filter(s -> !s.fecha().isAfter(hoy)) // US-08 CA5: no futuras
                .filter(s -> s.estado() == EstadoSesion.PROGRAMADA || s.estado() == EstadoSesion.DICTADA)
                .sorted((a, b) -> a.fecha().compareTo(b.fecha()))
                .toList();
    }

    @Override
    public StudentSummary resumenAlumno(String materiaId, String alumnoId) {
        List<SesionClase> dictadas =
                sesiones.values().stream()
                        .filter(s -> s.materiaId().equals(materiaId) && s.estado() == EstadoSesion.DICTADA)
                        .toList();
        double hDict = dictadas.stream().mapToDouble(SesionClase::bloqueHoras).sum();
        int hAsis = presentes.getOrDefault(alumnoId, Map.of()).entrySet().stream()
                .filter(e -> {
                    SesionClase s = sesiones.get(e.getKey());
                    return s != null && s.materiaId().equals(materiaId) && s.estado() == EstadoSesion.DICTADA;
                })
                .mapToInt(Map.Entry::getValue)
                .sum();
        double hFaltas = Math.max(0, hDict - hAsis);
        double pct = BrCalculations.attendancePercentage(hAsis, hDict, 0, 0);
        double margen = BrCalculations.marginHours(64, AppConfig.maxFaltasRatio());
        RiskLevel riesgo = BrCalculations.risk(hFaltas, margen);
        return new StudentSummary(
                materiaId, alumnoId, hDict, hAsis, hFaltas, 0, 0, pct, margen,
                BrCalculations.remainingHours(hFaltas, margen), riesgo);
    }

    @Override
    public void guardarAsistencia(String sesionId, Map<String, boolean[]> presentesPorAlumno) {
        SesionClase s = sesiones.get(sesionId);
        if (s == null) {
            throw new ApiException(ApiException.Kind.NOT_FOUND, "Sesión no encontrada");
        }
        if (s.fecha().isAfter(LocalDate.now())) {
            throw new ApiException(ApiException.Kind.VALIDATION, "No se permite tomar asistencia en fechas futuras");
        }
        if (s.estado() == EstadoSesion.FERIADA || s.estado() == EstadoSesion.SUSPENDIDA) {
            throw new ApiException(ApiException.Kind.VALIDATION, "Sesión feriada/suspendida");
        }
        for (var e : presentesPorAlumno.entrySet()) {
            boolean[] arr = e.getValue();
            if (arr == null || arr.length != s.bloqueHoras()) {
                throw new ApiException(
                        ApiException.Kind.VALIDATION,
                        "Se esperaban " + s.bloqueHoras() + " marcas por alumno");
            }
            int count = 0;
            for (boolean b : arr) {
                if (b) {
                    count++;
                }
            }
            presentes.computeIfAbsent(e.getKey(), k -> new ConcurrentHashMap<>()).put(sesionId, count);
        }
        // US-08 CA4: al guardar pasa a DICTADA
        sesiones.put(sesionId, new SesionClase(s.id(), s.materiaId(), s.fecha(), s.bloqueHoras(), EstadoSesion.DICTADA));
    }

    // Solo tests
    Map<String, SesionClase> sesionesInternas() {
        return new HashMap<>(sesiones);
    }
}

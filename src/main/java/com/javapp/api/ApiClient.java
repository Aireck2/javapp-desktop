package com.javapp.api;

import com.javapp.api.dto.LoginRequest;
import com.javapp.api.dto.Materia;
import com.javapp.api.dto.SesionClase;
import com.javapp.api.dto.SessionTokens;
import com.javapp.api.dto.StudentSummary;
import java.util.List;
import java.util.Map;

/**
 * Contrato HTTP (fase MVP: {@link MockApiClient}). Fase 2: {@code HttpApiClient} real.
 */
public interface ApiClient {

    SessionTokens login(LoginRequest req) throws ApiException;

    List<Materia> materiasForCurrentUser() throws ApiException;

    /** Solo fechas válidas US-08: pasadas/hoy, PROGRAMADA o DICTADA (nunca FERIADA/SUSPENDIDA/futura). */
    List<SesionClase> sesionesValidas(String materiaId) throws ApiException;

    StudentSummary resumenAlumno(String materiaId, String alumnoId) throws ApiException;

    /**
     * Guarda asistencia. {@code presentesPorAlumno}: alumnoId → array de N booleans
     * (uno por hora didáctica del bloque; true = Presente).
     * Al guardar la sesión pasa a DICTADA (US-08 CA4).
     */
    void guardarAsistencia(String sesionId, Map<String, boolean[]> presentesPorAlumno) throws ApiException;
}

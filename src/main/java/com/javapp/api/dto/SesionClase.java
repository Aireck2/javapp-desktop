package com.javapp.api.dto;

import java.time.LocalDate;

public record SesionClase(
        String id, String materiaId, LocalDate fecha, int bloqueHoras, EstadoSesion estado) {

    public enum EstadoSesion {
        PROGRAMADA,
        DICTADA,
        FERIADA,
        SUSPENDIDA
    }
}

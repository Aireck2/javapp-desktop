package com.javapp.api.dto;

import java.time.LocalDate;

public record ClassSession(
        String id, String courseId, LocalDate date, int blockHours, SessionStatus status) {

    public enum SessionStatus {
        PROGRAMADA,
        DICTADA,
        FERIADA,
        SUSPENDIDA
    }
}

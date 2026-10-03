package com.javapp.api.dto;

/** Active-cycle course with section context (mvp.md §3–§4). */
public record Course(
        String id,
        String code,
        String name,
        int totalHours,
        String section,
        String teacher,
        String schedule) {
    /** Compact constructor for fixtures without section detail. */
    public Course(String id, String code, String name, int totalHours) {
        this(id, code, name, totalHours, "SEC-01A", "Unassigned", "Mon–Wed 08:00–10:00");
    }
}

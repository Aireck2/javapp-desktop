package com.app.models;

import java.util.Objects;

/**
 * View Model inmutable para representar un curso en tarjetas y listas de la UI.
 * Desacoplado del protocolo de red / backend.
 */
public record CourseModel(
        String id,
        String code,
        String section,
        String name,
        String schedule,
        String classroom,
        String badgeType,
        int enrolledStudents,
        int completedHours,
        int totalHours
) {
    public CourseModel {
        id = id == null ? "" : id;
        code = code == null ? "" : code;
        section = section == null ? "" : section;
        name = name == null ? "" : name;
        schedule = schedule == null ? "" : schedule;
        classroom = classroom == null ? "Aula no especificada" : classroom;
        badgeType = badgeType == null ? "Regular" : badgeType;
        enrolledStudents = Math.max(0, enrolledStudents);
        completedHours = Math.max(0, completedHours);
        totalHours = Math.max(0, totalHours);
    }

    /**
     * Calcula el porcentaje de avance de horas dictadas/completadas sobre el total.
     * Retorna un entero entre 0 y 100.
     */
    public int getProgressPercentage() {
        if (totalHours <= 0) {
            return 0;
        }
        double ratio = (double) completedHours / totalHours;
        return (int) Math.clamp(Math.round(ratio * 100.0), 0, 100);
    }
}

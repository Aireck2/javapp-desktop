package com.javapp.common;

/**
 * Cálculos puros de negocio (BR-03 y BR-06). Sin dependencias JavaFX.
 *
 * <p>BR-03: %Asistencia = H_asistidas / (H_dictadas − H_justificadas − H_exentas) × 100
 * <p>BR-06: margen = H_exigibles_totales × %máx_faltas; consumo = horas en Falta.
 * Verde ≤50%, Amarillo 51–99%, Rojo ≥100% → DPI. Justificada/Exento no consumen.
 */
public final class BrCalculations {

    private BrCalculations() {}

    /** BR-03. Si no hay horas exigibles (denominador ≤ 0) retorna 100.0. Resultado en [0,100]. */
    public static double attendancePercentage(
            double attendedHours, double taughtHours, double excusedHours, double exemptHours) {
        double denominator = taughtHours - excusedHours - exemptHours;
        if (denominator <= 0) {
            return 100.0;
        }
        double pct = (attendedHours / denominator) * 100.0;
        return Math.min(100.0, Math.max(0.0, pct));
    }

    /** BR-06: margen de faltas en horas. */
    public static double marginHours(double totalRequiredHours, double maxFaltasRatio) {
        if (totalRequiredHours <= 0 || maxFaltasRatio <= 0) {
            return 0.0;
        }
        return totalRequiredHours * maxFaltasRatio;
    }

    /** BR-06: semáforo por consumo del margen. */
    public static RiskLevel risk(double consumedHours, double marginHours) {
        if (marginHours <= 0) {
            return consumedHours > 0 ? RiskLevel.ROJO : RiskLevel.VERDE;
        }
        double ratio = consumedHours / marginHours;
        if (ratio >= 1.0) {
            return RiskLevel.ROJO;
        }
        if (ratio > 0.5) {
            return RiskLevel.AMARILLO;
        }
        return RiskLevel.VERDE;
    }

    /** Horas restantes del margen (nunca negativo). */
    public static double remainingHours(double consumedHours, double marginHours) {
        return Math.max(0.0, marginHours - consumedHours);
    }
}

package com.javapp.api.dto;

import com.javapp.common.RiskLevel;

public record StudentSummary(
        String materiaId,
        String alumnoId,
        double hDictadas,
        double hAsistidas,
        double hFaltas,
        double hJustificadas,
        double hExentas,
        double porcentaje,
        double margenHoras,
        double restantesHoras,
        RiskLevel riesgo) {}

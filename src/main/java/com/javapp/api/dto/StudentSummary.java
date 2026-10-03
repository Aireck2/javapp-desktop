package com.javapp.api.dto;

import com.javapp.common.RiskLevel;

public record StudentSummary(
        String courseId,
        String studentId,
        double taughtHours,
        double attendedHours,
        double missedHours,
        double excusedHours,
        double exemptHours,
        double percentage,
        double marginHours,
        double remainingHours,
        RiskLevel risk) {}

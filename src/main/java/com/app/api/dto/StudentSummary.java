package com.app.api.dto;

import com.app.common.RiskLevel;

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

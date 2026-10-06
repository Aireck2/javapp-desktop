package com.app.models;

import com.app.common.RiskLevel;

/** Presentation data for one course card on the student's dashboard. */
public record StudentCourseModel(
    String code,
    String name,
    String section,
    String teacher,
    String schedule,
    double taughtHours,
    double attendedHours,
    double missedHours,
    double excusedHours,
    double exemptHours,
    double attendancePercentage,
    double remainingHours,
    double marginHours,
    RiskLevel risk) {}

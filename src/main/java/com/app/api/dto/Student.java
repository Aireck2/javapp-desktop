package com.app.api.dto;

/** Roster student (course detail view): institutional code + full name. */
public record Student(String id, String code, String fullName) {}

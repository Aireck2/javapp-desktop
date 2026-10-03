package com.app.api.dto;

import java.time.Instant;
import java.util.Set;

public record SessionTokens(
        String accessToken,
        String refreshToken,
        String username,
        String displayName,
        Set<String> groups,
        Instant expiresAt) {}

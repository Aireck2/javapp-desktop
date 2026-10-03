package com.app.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Sesión en RAM (singleton). Access Token nunca va a disco.
 * El Refresh Token vive en {@code TokenStore} (Preferences).
 */
public final class UserSession {

    private static final UserSession INSTANCE = new UserSession();

    private String accessToken;
    private String username;
    private String displayName;
    private Set<String> groups = new HashSet<>();
    private Instant expiresAt;
    private Instant lastActivity;

    private UserSession() {}

    public static UserSession getInstance() {
        return INSTANCE;
    }

    public synchronized void login(String accessToken, String username, String displayName,
            Set<String> groups, Instant expiresAt) {
        this.accessToken = accessToken;
        this.username = username;
        this.displayName = displayName;
        this.groups = groups == null ? new HashSet<>() : new HashSet<>(groups);
        this.expiresAt = expiresAt;
        this.lastActivity = Instant.now();
    }

    public synchronized void logout() {
        accessToken = null;
        username = null;
        displayName = null;
        groups = new HashSet<>();
        expiresAt = null;
        lastActivity = null;
    }

    public synchronized boolean isLoggedIn() {
        return accessToken != null && !isExpired();
    }

    public synchronized boolean isExpired() {
        if (accessToken == null) {
            return true;
        }
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public synchronized boolean hasRole(String role) {
        if (role == null) {
            return false;
        }
        return groups.stream().anyMatch(g -> g.equalsIgnoreCase(role));
    }

    public synchronized boolean hasAnyRole(String... roles) {
        for (String r : roles) {
            if (hasRole(r)) {
                return true;
            }
        }
        return false;
    }

    /** Marca actividad (llamar en cada interacción para control de inactividad US-01 CA6). */
    public synchronized void touch() {
        lastActivity = Instant.now();
    }

    public synchronized boolean isInactive(Duration timeout) {
        if (lastActivity == null) {
            return false;
        }
        return Duration.between(lastActivity, Instant.now()).compareTo(timeout) > 0;
    }

    public synchronized String getUsername() {
        return username;
    }

    public synchronized String getDisplayName() {
        return displayName == null ? username : displayName;
    }

    public synchronized Set<String> getGroups() {
        return Collections.unmodifiableSet(groups);
    }

    public synchronized String getAccessToken() {
        return accessToken;
    }

    // Solo tests: permite fijar lastActivity sin esperar.
    synchronized void setLastActivityForTest(Instant t) {
        this.lastActivity = t;
    }
}

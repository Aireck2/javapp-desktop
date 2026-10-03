package com.app.api.dto;

public record LoginRequest(String username, String password) {
    public LoginRequest {
        username = username == null ? "" : username.trim();
        password = password == null ? "" : password;
    }
}

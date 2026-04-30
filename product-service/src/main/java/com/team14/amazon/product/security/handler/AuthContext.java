package com.team14.amazon.product.security.handler;

import jakarta.servlet.http.HttpServletRequest;

public class AuthContext {
    private final HttpServletRequest request;
    private String token;
    private Long userId;
    private String role;
    private String requiredRole;
    private String errorReason;
    private int errorStatus;

    public AuthContext(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRequiredRole() {
        return requiredRole;
    }

    public void setRequiredRole(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public int getErrorStatus() {
        return errorStatus;
    }

    public void setError(int status, String reason) {
        this.errorStatus = status;
        this.errorReason = reason;
    }

    public boolean hasError() {
        return errorStatus != 0;
    }
}


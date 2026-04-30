package com.team14.amazon.shipping.security.handler;

import jakarta.servlet.http.HttpServletRequest;

public class AuthContext {
    private final HttpServletRequest request;

    // Core JWT Data
    private String token;
    private Long uid;          // IMPORTANT: Kept from your original because S4 needs it!
    private String subject;    // Aligned with reference (was email)
    private String tokenRole;  // Aligned with reference (was role)

    // Authorization Data
    private String requiredRole;

    // Error Handling State
    private String errorReason;
    private int errorStatus;

    public AuthContext(HttpServletRequest request) {
        this.request = request;
    }

    // --- Core Getters & Setters ---
    public HttpServletRequest getRequest() { return request; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUid() { return uid; }
    public void setUid(Long uid) { this.uid = uid; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTokenRole() { return tokenRole; }
    public void setTokenRole(String tokenRole) { this.tokenRole = tokenRole; }

    public String getRequiredRole() { return requiredRole; }
    public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }

    // --- Error Handling Methods ---
    public String getErrorReason() { return errorReason; }
    public int getErrorStatus() { return errorStatus; }

    public void setError(int status, String reason) {
        this.errorStatus = status;
        this.errorReason = reason;
    }

    public boolean hasError() {
        return errorStatus != 0;
    }
}
package com.team14.amazon.shipping.security.handler;

import jakarta.servlet.http.HttpServletRequest;

public class AuthContext {
    private final HttpServletRequest request;
    private String token;
    private String subject;
    private String tokenRole;
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

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTokenRole() {
        return tokenRole;
    }

    public void setTokenRole(String tokenRole) {
        this.tokenRole = tokenRole;
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

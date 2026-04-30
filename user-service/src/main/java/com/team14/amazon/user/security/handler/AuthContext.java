package com.team14.amazon.user.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import com.team14.amazon.user.model.User;

public class AuthContext {
    private final HttpServletRequest request;
    private String token;
    private User user;
    private String requiredRole;
    private String errorReason;
    private int errorStatus;

    public AuthContext(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletRequest getRequest() { return request; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getRequiredRole() { return requiredRole; }
    public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }

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

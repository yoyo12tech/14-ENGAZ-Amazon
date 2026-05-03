package com.team14.amazon.order.security.handler;

import jakarta.servlet.http.HttpServletRequest;

/**
 * DP-3 Chain of Responsibility: context object passed through the AuthHandler chain.
 * Stores the extracted token, parsed claims, and any error that short-circuits the chain.
 *
 * Order-service does not have a local User entity, so we store userId + role as primitives.
 */
public class AuthContext {

    private final HttpServletRequest request;

    private String  token;
    private String  email;
    private Long    userId;
    private String  role;
    private String  requiredRole;

    /** 0 means no error yet */
    private int    errorStatus;
    private String errorReason;

    public AuthContext(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletRequest getRequest()              { return request; }

    public String getToken()                            { return token; }
    public void   setToken(String token)                { this.token = token; }

    public String getEmail()                            { return email; }
    public void   setEmail(String email)                { this.email = email; }

    public Long   getUserId()                           { return userId; }
    public void   setUserId(Long userId)                { this.userId = userId; }

    public String getRole()                             { return role; }
    public void   setRole(String role)                  { this.role = role; }

    public String getRequiredRole()                     { return requiredRole; }
    public void   setRequiredRole(String requiredRole)  { this.requiredRole = requiredRole; }

    public void   setError(int status, String reason)   { this.errorStatus = status; this.errorReason = reason; }
    public boolean hasError()                           { return errorStatus != 0; }
    public int    getErrorStatus()                      { return errorStatus; }
    public String getErrorReason()                      { return errorReason; }
}

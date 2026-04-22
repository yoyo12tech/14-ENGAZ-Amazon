package com.team14.amazon.user.security;

import com.team14.amazon.user.model.User;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthContext {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private String token;
    private Claims claims;
    private User user;
    private String requiredRole;
    private boolean aborted;

    public AuthContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        this.aborted = false;
    }

    public HttpServletRequest getRequest() { return request; }
    public HttpServletResponse getResponse() { return response; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Claims getClaims() { return claims; }
    public void setClaims(Claims claims) { this.claims = claims; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getRequiredRole() { return requiredRole; }
    public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }
    public boolean isAborted() { return aborted; }
    public void setAborted(boolean aborted) { this.aborted = aborted; }
}
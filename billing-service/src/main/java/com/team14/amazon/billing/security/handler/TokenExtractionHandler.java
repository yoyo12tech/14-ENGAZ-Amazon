package com.team14.amazon.billing.security.handler;

import jakarta.servlet.http.HttpServletRequest;

public class TokenExtractionHandler extends AuthHandler {
    @Override
    public void handle(AuthContext ctx) {
        HttpServletRequest request = ctx.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.setError(401, "Token missing");
            return;
        }

        ctx.setToken(authHeader.substring(7).trim());
        next(ctx);
    }
}

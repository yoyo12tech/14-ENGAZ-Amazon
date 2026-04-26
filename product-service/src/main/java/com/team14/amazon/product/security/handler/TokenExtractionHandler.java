package com.team14.amazon.product.security.handler;

public class TokenExtractionHandler extends AuthHandler {
    @Override
    public void handle(AuthContext ctx) {
        String authHeader = ctx.getRequest().getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.setError(401, "Token missing");
            return;
        }
        String token = authHeader.substring(7).trim();
        if (token.isBlank()) {
            ctx.setError(401, "Token missing");
            return;
        }
        ctx.setToken(token);
        next(ctx);
    }
}


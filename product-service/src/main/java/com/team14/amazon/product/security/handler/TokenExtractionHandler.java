package com.team14.amazon.product.security.handler;

public class TokenExtractionHandler extends AuthHandler {
    @Override
    public void handle(AuthContext ctx) {
        String authHeader = ctx.getRequest().getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.setError(401, "Token missing");
            return;
        }
        ctx.setToken(authHeader.substring(7));
        next(ctx);
    }
}


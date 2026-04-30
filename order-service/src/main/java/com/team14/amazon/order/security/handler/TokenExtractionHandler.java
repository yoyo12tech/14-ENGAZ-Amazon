package com.team14.amazon.order.security.handler;

/**
 * DP-3: Chain of Responsibility — Handler 1.
 * Extracts the Bearer token from the Authorization header.
 * Returns 401 if the header is missing or malformed.
 */
public class TokenExtractionHandler extends AuthHandler {

    @Override
    public void handle(AuthContext ctx) {
        String authHeader = ctx.getRequest().getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.setError(401, "Authorization header missing or malformed");
            return;
        }
        ctx.setToken(authHeader.substring(7));
        next(ctx);
    }
}

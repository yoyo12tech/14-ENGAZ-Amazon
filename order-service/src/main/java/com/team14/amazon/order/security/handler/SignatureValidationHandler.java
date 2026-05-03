package com.team14.amazon.order.security.handler;

import com.team14.amazon.order.security.JwtService;

/**
 * DP-3: Chain of Responsibility — Handler 2.
 * Verifies the JWT signature and expiration.
 * Returns 401 if invalid or expired.
 */
public class SignatureValidationHandler extends AuthHandler {

    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void handle(AuthContext ctx) {
        if (!jwtService.validateToken(ctx.getToken())) {
            ctx.setError(401, "Invalid or expired JWT token");
            return;
        }
        // Populate context with claims
        ctx.setEmail(jwtService.extractEmail(ctx.getToken()));
        ctx.setUserId(jwtService.extractUserId(ctx.getToken()));
        ctx.setRole(jwtService.extractRole(ctx.getToken()));
        next(ctx);
    }
}

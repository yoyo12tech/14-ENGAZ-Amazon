package com.team14.amazon.product.security.handler;

import com.team14.amazon.product.security.JwtService;

public class SignatureValidationHandler extends AuthHandler {
    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void handle(AuthContext ctx) {
        if (!jwtService.validateToken(ctx.getToken())) {
            ctx.setError(401, "Invalid or expired token");
            return;
        }
        try {
            ctx.setUserId(jwtService.extractUserId(ctx.getToken()));
            ctx.setRole(jwtService.extractRole(ctx.getToken()));
        } catch (Exception ex) {
            ctx.setError(401, "Invalid token claims");
            return;
        }

        if (ctx.getRole() == null || ctx.getRole().isBlank()) {
            ctx.setError(401, "Invalid token claims");
            return;
        }
        next(ctx);
    }
}


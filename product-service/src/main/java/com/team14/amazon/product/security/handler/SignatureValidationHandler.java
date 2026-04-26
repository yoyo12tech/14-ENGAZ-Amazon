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
        ctx.setUserId(jwtService.extractUserId(ctx.getToken()));
        ctx.setRole(jwtService.extractRole(ctx.getToken()));
        next(ctx);
    }
}


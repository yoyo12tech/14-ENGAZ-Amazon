package com.team14.amazon.user.security.handler;

import com.team14.amazon.user.security.JwtService;

public class SignatureValidationHandler extends AuthHandler {
    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void handle(AuthContext ctx) {
        if (!jwtService.validateToken(ctx.getToken())) {
            ctx.setError(401, "Invalid token signature or expired");
            return;
        }
        next(ctx);
    }
}

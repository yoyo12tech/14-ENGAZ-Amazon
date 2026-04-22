package com.team14.amazon.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

public class SignatureValidationHandler extends AuthHandler {
    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void handle(AuthContext ctx) {
        try {
            Claims claims = jwtService.validateToken(ctx.getToken());
            ctx.setClaims(claims);
            handleNext(ctx);
        } catch (JwtException | IllegalArgumentException e) {
            ctx.setAborted(true);
            ctx.getResponse().setStatus(401);
        }
    }
}
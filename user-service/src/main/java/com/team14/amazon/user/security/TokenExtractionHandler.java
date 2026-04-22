package com.team14.amazon.user.security;

public class TokenExtractionHandler extends AuthHandler {
    @Override
    public void handle(AuthContext ctx) {
        String header = ctx.getRequest().getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            ctx.setAborted(true);
            ctx.getResponse().setStatus(401);
            return;
        }
        ctx.setToken(header.substring(7));
        handleNext(ctx);
    }
}
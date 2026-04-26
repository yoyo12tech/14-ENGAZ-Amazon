package com.team14.amazon.shipping.security.handler;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import org.springframework.stereotype.Component;

@Component
public class TokenExtractionHandler extends AuthHandler {

    @Override
    public void handle(AuthContext ctx) {
        String authHeader = ctx.getRequest().getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Set the error in the context and DO NOT call next(ctx)
            ctx.setError(401, "Missing or invalid Authorization header");
            return;
        }

        // Strip "Bearer " (7 characters) and save to context
        ctx.setToken(authHeader.substring(7));

        // Pass to the SignatureValidationHandler
        next(ctx);
    }
}
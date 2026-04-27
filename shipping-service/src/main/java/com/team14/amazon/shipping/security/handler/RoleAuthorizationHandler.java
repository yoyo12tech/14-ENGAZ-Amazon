package com.team14.amazon.shipping.security.handler;

import org.springframework.stereotype.Component;

@Component
public class RoleAuthorizationHandler extends AuthHandler {

    @Override
    public void handle(AuthContext ctx) {
        String required = ctx.getRequiredRole();

        // If no role is required for this endpoint, pass through
        if (required == null || required.isBlank()) {
            next(ctx);
            return;
        }

        String tokenRole = ctx.getTokenRole();
        if (tokenRole == null || !tokenRole.equals(required)) {
            ctx.setError(403, "Forbidden: insufficient role");
            return;
        }

        next(ctx);
    }
}
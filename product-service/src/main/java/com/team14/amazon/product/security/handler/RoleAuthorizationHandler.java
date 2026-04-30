package com.team14.amazon.product.security.handler;

public class RoleAuthorizationHandler extends AuthHandler {
    @Override
    public void handle(AuthContext ctx) {
        if (ctx.getRequiredRole() != null && !ctx.getRequiredRole().equals(ctx.getRole())) {
            ctx.setError(403, "Insufficient role");
            return;
        }
        next(ctx);
    }
}


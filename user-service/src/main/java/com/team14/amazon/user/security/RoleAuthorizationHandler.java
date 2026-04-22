package com.team14.amazon.user.security;

public class RoleAuthorizationHandler extends AuthHandler {
    @Override
    public void handle(AuthContext ctx) {
        String userRole = ctx.getUser().getRole().name();
        String requiredRole = ctx.getRequiredRole();

        // Admin can access everything
        if ("ADMIN".equals(userRole)) {
            handleNext(ctx);
            return;
        }

        // If CUSTOMER, but ADMIN required -> 403
        if ("ADMIN".equals(requiredRole)) {
            ctx.setAborted(true);
            ctx.getResponse().setStatus(403);
            return;
        }

        handleNext(ctx);
    }
}
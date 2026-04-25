package com.team14.amazon.user.security.handler;

public class RoleAuthorizationHandler extends AuthHandler {
    @Override
    public void handle(AuthContext ctx) {
        if (ctx.getRequiredRole() != null) {
            String userRole = ctx.getUser().getRole().name();
            if (!userRole.equals(ctx.getRequiredRole())) {
                ctx.setError(403, "Insufficient role");
                return;
            }
        }
        next(ctx);
    }
}

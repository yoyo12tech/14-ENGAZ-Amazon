package com.team14.amazon.billing.security.handler;

public class RoleAuthorizationHandler extends AuthHandler {
    @Override
    public void handle(AuthContext ctx) {
        if (ctx.getRequiredRole() != null) {
            String granted = ctx.getTokenRole();
            if (granted == null || !granted.equals(ctx.getRequiredRole())) {
                ctx.setError(403, "Insufficient role");
                return;
            }
        }
        next(ctx);
    }
}

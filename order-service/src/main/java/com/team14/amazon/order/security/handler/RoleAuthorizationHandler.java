package com.team14.amazon.order.security.handler;

/**
 * DP-3 Chain Handler 4: checks the required role (if any is set on the context).
 * Returns 403 if the token's role is insufficient.
 * ADMIN always passes — ADMIN is a superset of CUSTOMER.
 */
public class RoleAuthorizationHandler extends AuthHandler {

    @Override
    public void handle(AuthContext ctx) {
        String required = ctx.getRequiredRole();
        if (required != null && !required.isBlank()) {
            String userRole = ctx.getRole();
            // ADMIN passes everything; otherwise role must match exactly
            if (!"ADMIN".equals(userRole) && !required.equals(userRole)) {
                ctx.setError(403, "Insufficient role");
                return;
            }
        }
        next(ctx);
    }
}

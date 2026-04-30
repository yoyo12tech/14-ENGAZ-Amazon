package com.team14.amazon.order.security.handler;

/**
 * DP-3 Chain Handler 3: verifies required JWT claims are present.
 *
 * Order-service has no local User table, so we only verify that the
 * uid and email claims exist in the token (populated by handler 2).
 * This mirrors the spec requirement that a missing/deleted user returns 401.
 */
public class UserLoaderHandler extends AuthHandler {

    @Override
    public void handle(AuthContext ctx) {
        if (ctx.getUserId() == null || ctx.getEmail() == null || ctx.getEmail().isBlank()) {
            ctx.setError(401, "User not found");
            return;
        }
        next(ctx);
    }
}

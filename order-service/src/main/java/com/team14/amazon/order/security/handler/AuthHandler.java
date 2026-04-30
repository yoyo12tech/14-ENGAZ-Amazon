package com.team14.amazon.order.security.handler;

/**
 * DP-3: Chain of Responsibility — abstract handler.
 * Concrete handlers: TokenExtractionHandler, SignatureValidationHandler,
 * UserLoaderHandler, RoleAuthorizationHandler.
 */
public abstract class AuthHandler {

    protected AuthHandler next;

    public void setNext(AuthHandler next) {
        this.next = next;
    }

    public abstract void handle(AuthContext ctx);

    protected void next(AuthContext ctx) {
        if (next != null && !ctx.hasError()) {
            next.handle(ctx);
        }
    }
}

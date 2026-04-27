package com.team14.amazon.shipping.security.handler;

public abstract class AuthHandler {
    private AuthHandler next;

    public AuthHandler setNext(AuthHandler next) {
        this.next = next;
        return next;
    }

    protected void next(AuthContext ctx) {
        if (next != null && !ctx.hasError()) {
            next.handle(ctx);
        }
    }

    public abstract void handle(AuthContext ctx);
}
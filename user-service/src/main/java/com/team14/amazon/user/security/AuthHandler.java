package com.team14.amazon.user.security;

public abstract class AuthHandler {
    protected AuthHandler next;

    public AuthHandler setNext(AuthHandler next) {
        this.next = next;
        return next;
    }

    public abstract void handle(AuthContext ctx);

    protected void handleNext(AuthContext ctx) {
        if (!ctx.isAborted() && next != null) {
            next.handle(ctx);
        }
    }
}
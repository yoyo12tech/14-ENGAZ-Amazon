package com.team14.amazon.user.security.handler;

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

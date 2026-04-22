package com.team14.amazon.user.security;

import com.team14.amazon.user.repository.UserRepository;

public class UserLoaderHandler extends AuthHandler {
    private final UserRepository userRepository;

    public UserLoaderHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void handle(AuthContext ctx) {
        Number uidNum = ctx.getClaims().get("uid", Number.class);
        if (uidNum == null) {
            ctx.setAborted(true);
            ctx.getResponse().setStatus(401);
            return;
        }
        Long uid = uidNum.longValue();
        userRepository.findById(uid).ifPresentOrElse(user -> {
            ctx.setUser(user);
            handleNext(ctx);
        }, () -> {
            ctx.setAborted(true);
            ctx.getResponse().setStatus(401);
        });
    }
}
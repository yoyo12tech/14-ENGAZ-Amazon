package com.team14.amazon.user.security.handler;

import com.team14.amazon.user.model.User;
import com.team14.amazon.user.repository.UserRepository;
import com.team14.amazon.user.security.JwtService;

public class UserLoaderHandler extends AuthHandler {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public UserLoaderHandler(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void handle(AuthContext ctx) {
        String email = jwtService.extractEmail(ctx.getToken());
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            ctx.setError(401, "User not found");
            return;
        }

        ctx.setUser(user);
        next(ctx);
    }
}

package com.team14.amazon.user.controller;

import com.team14.amazon.user.dto.AuthResponseDTO;
import com.team14.amazon.user.dto.LoginRequestDTO;
import com.team14.amazon.user.dto.RegisterRequestDTO;
import com.team14.amazon.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponseDTO register(@jakarta.validation.Valid @RequestBody RegisterRequestDTO request) {
        return userService.registerUser(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponseDTO login(@jakarta.validation.Valid @RequestBody LoginRequestDTO request) {
        return userService.loginUser(request);
    }
}

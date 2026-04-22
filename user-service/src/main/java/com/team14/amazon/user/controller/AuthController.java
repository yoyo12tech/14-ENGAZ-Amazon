package com.team14.amazon.user.controller;

import com.team14.amazon.user.dto.AuthResponse;
import com.team14.amazon.user.dto.RegisterRequest;
import com.team14.amazon.user.model.User;
import com.team14.amazon.user.repository.UserRepository;
import com.team14.amazon.user.security.JwtConfigurationManager;
import com.team14.amazon.user.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank() ||
            request.getPassword() == null || request.getPassword().isBlank() ||
            request.getName() == null || request.getName().isBlank() ||
            request.getPhone() == null || request.getPhone().isBlank()) {
            return ResponseEntity.badRequest().body("Blank fields are not allowed");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent() ||
            userRepository.findByPhone(request.getPhone()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email or phone already exists");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(User.Role.CUSTOMER);

        user = userRepository.save(user);

        String token = jwtService.generateToken(user);
        long expiresIn = JwtConfigurationManager.getInstance().getExpirationMs();

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token, expiresIn));
    }
}
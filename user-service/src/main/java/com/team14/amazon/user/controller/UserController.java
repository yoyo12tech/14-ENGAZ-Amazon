package com.team14.amazon.user.controller;

import com.team14.amazon.user.dto.UserProfileDTO;
import com.team14.amazon.user.model.User;
import com.team14.amazon.user.model.UserRole;
import com.team14.amazon.user.model.UserStatus;
import com.team14.amazon.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import com.team14.amazon.user.dto.TopBuyerDTO;
import com.team14.amazon.user.dto.UserOrderSummaryDTO;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import com.team14.amazon.user.dto.ActivityFeedDTO;
import com.team14.amazon.user.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    public UserController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }


    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
        try {
            Optional<User> result = userService.updateUser(id, updatedUser);

            if (result.isPresent()) {
                return ResponseEntity.status(HttpStatus.OK).body(result.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            }

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userService.deleteUser(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String role
    ) {
        try {
            return ResponseEntity.ok(userService.searchUsers(name, email, role));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/reports/top-buyers")
    public ResponseEntity<?> getTopBuyers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam Integer limit) {
        try {
            List<TopBuyerDTO> topBuyers = userService.getTopBuyers(startDate, endDate, limit);
            return ResponseEntity.ok(topBuyers);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/preferences/search")
    public ResponseEntity<?> searchByPreference(@RequestParam String key, @RequestParam String value) {
        if (key == null || key.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Key must not be blank");
        }
        try {
            return ResponseEntity.ok(userService.findUsersByPreference(key, value));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
    
    @Transactional
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }


    @GetMapping("/{id}/profile")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable Long id) {
        UserProfileDTO profile = userService.getUserProfile(id);
        return ResponseEntity.ok(profile);
        }

    @GetMapping("/preferences/language")
    public ResponseEntity<List<User>> getUsersByLanguageAndMinOrders(
            @RequestParam String lang,
            @RequestParam(defaultValue = "0") int minOrders) {

        return ResponseEntity.ok(
                userService.findUsersByLanguageAndMinOrders(lang, minOrders)
        );
    }

    @PutMapping("/{id}/preferences")
    public ResponseEntity<?> updatePreferences(@PathVariable Long id,@RequestBody Map<String, Object> preferences) {
        try {
            User updatedUser = userService.updateUserPreferences(id, preferences);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/order-summary")
    public ResponseEntity<?> getUserOrderSummary(@PathVariable Long id) {
        try {
            UserOrderSummaryDTO summary = userService.getUserOrderSummary(id);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<User> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String roleStr = body.get("role");
        if (roleStr == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            UserRole role = UserRole.valueOf(roleStr.toUpperCase());
            User updatedUser = userService.updateUserRole(id, role);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<?> getActivityFeed(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            // Extract caller identity from JWT (token already validated by filter)
            String authHeader = request.getHeader("Authorization");
            String token = authHeader.substring(7); // strip "Bearer "
            Long callerUid = jwtService.extractUserId(token);
            String callerRole = jwtService.extractRole(token);

            ActivityFeedDTO feed = userService.getActivityFeed(id, callerUid, callerRole, page, size);
            return ResponseEntity.ok(feed);
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }
    }
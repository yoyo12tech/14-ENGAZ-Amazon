package com.team14.amazon.user.controller;

import com.team14.amazon.user.dto.UserProfileDTO;
import com.team14.amazon.user.model.User;
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

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
    public ResponseEntity<List<User>> searchByPreference(@RequestParam String key, @RequestParam String value) {
        return ResponseEntity.ok(userService.findUsersByPreference(key, value));
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
}
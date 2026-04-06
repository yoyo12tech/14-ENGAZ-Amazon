package com.team14.amazon.user.service;

import com.team14.amazon.user.model.User;
import com.team14.amazon.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User createUser(User user) {
        
            if (userRepository.findByEmail(user.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email is already in use.");
            }
            if (userRepository.findByPhone(user.getPhone()).isPresent()) {
                throw new IllegalArgumentException("Phone number is already in use.");
            }
            
            return userRepository.save(user);
    }

    public Optional<User> updateUser(Long id, User updatedUser) {
        return userRepository.findById(id).map(existingUser -> {
            
            if (!existingUser.getEmail().equals(updatedUser.getEmail()) && 
                userRepository.findByEmail(updatedUser.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email is already in use.");
            }
            
            if (!existingUser.getPhone().equals(updatedUser.getPhone()) && 
                userRepository.findByPhone(updatedUser.getPhone()).isPresent()) {
                throw new IllegalArgumentException("Phone number is already in use.");
            }

            existingUser.setName(updatedUser.getName());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setPhone(updatedUser.getPhone());
            existingUser.setRole(updatedUser.getRole());
            existingUser.setStatus(updatedUser.getStatus());
            
            existingUser.setPreferences(updatedUser.getPreferences());
            
            if (updatedUser.getPassword() != null && !updatedUser.getPassword().trim().isEmpty()) {
                existingUser.setPassword(updatedUser.getPassword());
            }

            return userRepository.save(existingUser);
        });
    }
    
    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<User> searchUsers(String name, String email, String role) {
        String normalizedName = normalize(name);
        String normalizedEmail = normalize(email);
        String normalizedRole = normalizeRole(role);
    
        return userRepository.searchUsers(normalizedName, normalizedEmail, normalizedRole);
    }
    
    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
    
    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
    
        try {
            return User.Role.valueOf(role.trim().toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role. Must be CUSTOMER or ADMIN.");
        }
    }

    public User updateUserPreferences(Long id, Map<String, Object> newPreferences) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getPreferences() == null) {
            user.setPreferences(new HashMap<>());
        }

        Map<String, Object> existingPreferences = user.getPreferences();

        for (Map.Entry<String, Object> entry : newPreferences.entrySet()) {
            existingPreferences.put(entry.getKey(), entry.getValue());
        }

        user.setPreferences(existingPreferences);

        return userRepository.save(user);
    }
}

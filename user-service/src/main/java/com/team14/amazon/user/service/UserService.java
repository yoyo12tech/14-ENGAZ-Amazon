package com.team14.amazon.user.service;

import com.team14.amazon.user.model.User;
import com.team14.amazon.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
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

    public List<User> findUsersByPreference(String key, String value) {
        if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Key and value must not be blank");
        }
        return userRepository.findAll().stream()
                .filter(user -> user.getPreferences() != null && value.equals(user.getPreferences().get(key)))
                .toList();
    }
}

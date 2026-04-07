package com.team14.amazon.user.service;

import com.team14.amazon.user.dto.UserProfileDTO;
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

    // ==========================================
    // MILESTONE 1 FEATURES
    // ==========================================

    /**
     * [S1-F8] Get User Profile with Shipping Addresses
     */
    public UserProfileDTO getUserProfile(Long userId) {
        // 1. Find the user or throw 404
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 2. Map data to the DTO
        UserProfileDTO profileDTO = new UserProfileDTO();
        profileDTO.setUserId(user.getId());
        profileDTO.setName(user.getName());
        profileDTO.setEmail(user.getEmail());
        profileDTO.setPhone(user.getPhone());
        profileDTO.setPreferences(user.getPreferences());

        // 3. Attach the addresses and calculate the total count
        profileDTO.setShippingAddresses(user.getShippingAddresses());

        // Safe check in case the list is null
        int addressCount = (user.getShippingAddresses() != null) ? user.getShippingAddresses().size() : 0;
        profileDTO.setTotalAddresses(addressCount);

        return profileDTO;
    }
}
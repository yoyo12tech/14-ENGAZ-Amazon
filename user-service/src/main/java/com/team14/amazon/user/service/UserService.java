package com.team14.amazon.user.service;

import com.team14.amazon.user.dto.UserProfileDTO;
import com.team14.amazon.user.model.ShippingAddress;
import com.team14.amazon.user.model.User;
import com.team14.amazon.user.repository.ShippingAddressRepository;
import com.team14.amazon.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ShippingAddressRepository shippingAddressRepository;

    // Updated constructor to inject the ShippingAddressRepository
    public UserService(UserRepository userRepository, ShippingAddressRepository shippingAddressRepository) {
        this.userRepository = userRepository;
        this.shippingAddressRepository = shippingAddressRepository;
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

    @Transactional
    public User setDefaultShippingAddress(Long userId, Long addressId) {
        // 1. Find the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 2. Find the target shipping address
        ShippingAddress targetAddress = shippingAddressRepository.findById(addressId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipping address not found"));

        // 3. Verify the address belongs to this user
        if (!targetAddress.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address does not belong to this user");
        }

        // 4. Set isDefault = false on ALL of the user's existing addresses
        for (ShippingAddress address : user.getShippingAddresses()) {
            address.setDefault(false);
        }

        // 5. Set the target address to true
        targetAddress.setDefault(true);

        // 6. Save and return the updated user
        return userRepository.save(user);
    }

    public List<User> findUsersByLanguageAndMinOrders(String lang, Integer minOrders) {
        // Validate lang is not blank per requirement
        if (lang == null || lang.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Language cannot be blank");
        }
        return userRepository.findUsersByLanguageAndMinOrders(lang, minOrders);
    }
}
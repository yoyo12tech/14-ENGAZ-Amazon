package com.team14.amazon.user.service;

import com.team14.amazon.user.model.User;
import com.team14.amazon.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.team14.amazon.user.dto.TopBuyerDTO;

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

    public List<TopBuyerDTO> getTopBuyers(LocalDate startDate, LocalDate endDate, Integer limit) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        
        List<Object[]> rawResults = userRepository.findTopBuyers(startDateTime, endDateTime, limit);
        
        return rawResults.stream().map(row -> new TopBuyerDTO(
                ((Number) row[0]).longValue(),
                (String) row[1],
                row[2] != null ? ((Number) row[2]).doubleValue() : 0.0,
                ((Number) row[3]).longValue()
        )).collect(Collectors.toList());
    }
}

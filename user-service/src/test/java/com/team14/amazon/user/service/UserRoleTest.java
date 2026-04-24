package com.team14.amazon.user.service;

import com.team14.amazon.user.model.User;
import com.team14.amazon.user.repository.UserRepository;
import com.team14.amazon.user.repository.ShippingAddressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserRoleTest {

    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShippingAddressRepository shippingAddressRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, shippingAddressRepository, passwordEncoder);
    }

    @Test
    public void testRoleIsDefaultedToCustomer() {
        User adminAttempt = new User();
        adminAttempt.setEmail("badactor@example.com");
        adminAttempt.setPhone("0000000000");
        adminAttempt.setPassword("password");
        adminAttempt.setRole(User.Role.ADMIN); // Attempting to register as ADMIN

        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        User createdUser = userService.createUser(adminAttempt);

        assertEquals(User.Role.CUSTOMER, createdUser.getRole(), "Role must be defaulted to CUSTOMER regardless of input");
    }

    @Test
    public void testUpdateUserRole() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setRole(User.Role.CUSTOMER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        User updatedUser = userService.updateUserRole(1L, User.Role.ADMIN);

        assertEquals(User.Role.ADMIN, updatedUser.getRole(), "Role should be updated to ADMIN");
        verify(userRepository).save(existingUser);
    }
}

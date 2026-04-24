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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PasswordHashingTest {

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
    public void testPasswordIsHashedOnCreate() {
        User rawUser = new User();
        rawUser.setEmail("test@example.com");
        rawUser.setPhone("1234567890");
        rawUser.setPassword("rawPassword123");

        when(passwordEncoder.encode("rawPassword123")).thenReturn("$2a$10$hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        userService.createUser(rawUser);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User savedUser = userCaptor.getValue();
        assertEquals("$2a$10$hashedPassword", savedUser.getPassword());
        assertNotEquals("rawPassword123", savedUser.getPassword());
    }

    @Test
    public void testPasswordIsHashedOnUpdate() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setPassword("$2a$10$oldHash");
        existingUser.setEmail("old@example.com");
        existingUser.setPhone("1234567890");

        User updateRequest = new User();
        updateRequest.setEmail("old@example.com");
        updateRequest.setPhone("1234567890");
        updateRequest.setPassword("newPassword456");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newPassword456")).thenReturn("$2a$10$newHashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        userService.updateUser(1L, updateRequest);

        verify(passwordEncoder).encode("newPassword456");
        assertEquals("$2a$10$newHashedPassword", existingUser.getPassword());
    }

    @Test
    public void testPasswordSerializationAccess() throws Exception {
        User user = new User();
        user.setPassword("secret");
        user.setEmail("test@test.com");

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(user);

        assertFalse(json.contains("password"), "JSON should not contain password field");
        
        User deserialized = mapper.readValue("{\"password\":\"newSecret\",\"email\":\"test@test.com\"}", User.class);
        assertEquals("newSecret", deserialized.getPassword(), "Should be able to deserialize password");
    }
}

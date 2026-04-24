package com.team14.amazon.user.service;

import com.team14.amazon.user.dto.AuthResponseDTO;
import com.team14.amazon.user.dto.RegisterRequestDTO;
import com.team14.amazon.user.model.User;
import com.team14.amazon.user.observer.EntityObserver;
import com.team14.amazon.user.repository.ShippingAddressRepository;
import com.team14.amazon.user.repository.UserRepository;
import com.team14.amazon.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthRegistrationServiceTest {

    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShippingAddressRepository shippingAddressRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EntityObserver mongoEventLogger;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, shippingAddressRepository, passwordEncoder, jwtService, mongoEventLogger);
        userService.init(); // to register mongoEventLogger
    }

    @Test
    void testRegisterUserSuccess() {
        RegisterRequestDTO request = new RegisterRequestDTO("Ahmed", "ahmed@example.com", "pass", "123");
        
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.findByPhone(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(any(), any(), any())).thenReturn("mockToken");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponseDTO response = userService.registerUser(request);

        assertNotNull(response);
        assertEquals("mockToken", response.getToken());
        verify(userRepository).save(any(User.class));
        verify(mongoEventLogger).onEvent(eq("REGISTERED"), any());
    }

    @Test
    void testRegisterUserConflict() {
        RegisterRequestDTO request = new RegisterRequestDTO("Ahmed", "ahmed@example.com", "pass", "123");
        
        when(userRepository.findByEmail("ahmed@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(ResponseStatusException.class, () -> userService.registerUser(request));
    }

    @Test
    void testRegisterUserPhoneConflict() {
        RegisterRequestDTO request = new RegisterRequestDTO("Ahmed", "ahmed2@example.com", "pass", "123");
        
        when(userRepository.findByEmail("ahmed2@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("123")).thenReturn(Optional.of(new User()));

        assertThrows(ResponseStatusException.class, () -> userService.registerUser(request));
    }

    @Test
    void testRegisterUserBlank() {
        RegisterRequestDTO request = new RegisterRequestDTO("", "", "", "");
        assertThrows(ResponseStatusException.class, () -> userService.registerUser(request));
    }
}

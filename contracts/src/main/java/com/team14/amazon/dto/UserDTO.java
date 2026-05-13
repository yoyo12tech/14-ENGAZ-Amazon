package com.team14.amazon.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record UserDTO(
        Long id,
        String name,
        String email,
        String phone,
        String role,
        String status,
        Map<String, Object> preferences,
        LocalDateTime createdAt
) { }

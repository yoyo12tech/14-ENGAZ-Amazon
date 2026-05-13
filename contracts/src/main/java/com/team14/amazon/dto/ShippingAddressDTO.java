package com.team14.amazon.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ShippingAddressDTO(
        Long id,
        Long userId,
        String label,
        String streetAddress,
        String city,
        String country,
        String zipCode,
        boolean isDefault,
        Map<String, Object> metadata,
        LocalDateTime createdAt
) {
}

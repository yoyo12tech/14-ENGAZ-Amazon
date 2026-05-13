package com.team14.amazon.contracts.dto;

import java.time.LocalDateTime;

public record OrderDTO(
        Long id,
        Long userId,
        Long shippingAddressId,
        String status,
        Double totalAmount,
        LocalDateTime orderedAt,
        LocalDateTime deliveredAt
) {}
package com.team14.amazon.contracts.dto;

import java.time.LocalDateTime;

public record ShipmentDTO(
        Long id,
        Long orderId,
        String carrier,
        String trackingNumber,
        String status,
        LocalDateTime lastUpdate,
        LocalDateTime actualDelivery
) {}
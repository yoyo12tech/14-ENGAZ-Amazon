package com.team14.amazon.shipping.dto;

import com.team14.amazon.shipping.model.Shipment;

import java.time.LocalDateTime;
import java.util.Map;

public record ShipmentDTO (
        Long id,
        Long orderId,
        String carrier,
        String trackingNumber,
        Shipment.Status status,
        Double latitude,
        Double longitude,
        LocalDateTime estimatedDelivery,
        LocalDateTime actualDelivery,
        Map<String, Object> metadata,
        LocalDateTime lastUpdate,
        LocalDateTime createdAt
) {}

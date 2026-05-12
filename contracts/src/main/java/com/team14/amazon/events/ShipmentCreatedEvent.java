package com.team14.amazon.events;

public record ShipmentCreatedEvent(Long shipmentId, Long orderId, String carrier, String trackingNumber) {}

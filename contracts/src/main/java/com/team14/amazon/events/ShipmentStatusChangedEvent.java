package com.team14.amazon.events;

public record ShipmentStatusChangedEvent(Long shipmentId, Long orderId, String newStatus) {}

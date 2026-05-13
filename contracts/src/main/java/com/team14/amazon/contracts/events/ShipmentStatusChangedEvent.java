package com.team14.amazon.contracts.events;

public record ShipmentStatusChangedEvent(Long shipmentId, Long orderId, String newStatus) {}

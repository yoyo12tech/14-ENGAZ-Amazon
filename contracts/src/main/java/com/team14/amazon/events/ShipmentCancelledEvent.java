package com.team14.amazon.events;

public record ShipmentCancelledEvent(Long shipmentId, Long orderId) {}

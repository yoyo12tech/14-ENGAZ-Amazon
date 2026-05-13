package com.team14.amazon.contracts.events;

public record OrderCompletedEvent(Long orderId, Long userId, Long shippingAddressId, Double totalAmount) {}

package com.team14.amazon.events;

public record OrderCompletedEvent(Long orderId, Long userId, Long shippingAddressId, Double totalAmount) {}

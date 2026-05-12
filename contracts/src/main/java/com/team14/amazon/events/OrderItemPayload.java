package com.team14.amazon.events;

public record OrderItemPayload(Long productId, Integer quantity, Double priceAtPurchase) {}

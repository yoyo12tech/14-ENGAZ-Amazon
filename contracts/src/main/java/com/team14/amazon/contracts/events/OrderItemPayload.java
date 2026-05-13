package com.team14.amazon.contracts.events;

public record OrderItemPayload(Long productId, Integer quantity, Double priceAtPurchase) {}

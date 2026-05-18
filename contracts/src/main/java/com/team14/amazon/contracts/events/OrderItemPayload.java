package com.team14.amazon.contracts.events;

import java.io.Serializable;

public record OrderItemPayload
        (Long productId, Integer quantity, Double priceAtPurchase) implements Serializable {}
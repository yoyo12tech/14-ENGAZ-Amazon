package com.team14.amazon.contracts.events;

import java.io.Serializable;
import java.util.List;

public record OrderPlacedEvent
        (Long orderId, Long userId, Long shippingAddressId, List<OrderItemPayload> items) implements Serializable {}

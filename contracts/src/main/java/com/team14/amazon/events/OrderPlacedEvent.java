package com.team14.amazon.events;

import java.util.List;

public record OrderPlacedEvent(Long orderId, Long userId, Long shippingAddressId, List<OrderItemPayload> items) {}

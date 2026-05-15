package com.team14.amazon.contracts.events;

import java.util.List;

public record OrderPlacedEvent(Long orderId, Long userId, Long shippingAddressId, List<OrderItemPayload> items) {}

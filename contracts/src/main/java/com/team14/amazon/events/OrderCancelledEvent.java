package com.team14.amazon.events;

import java.util.List;

public record OrderCancelledEvent(Long orderId, Long userId, List<OrderItemPayload> restoredItems, String reason) {}

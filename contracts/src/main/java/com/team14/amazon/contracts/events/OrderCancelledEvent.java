package com.team14.amazon.contracts.events;

import java.io.Serializable;
import java.util.List;

public record OrderCancelledEvent(Long orderId, Long userId, List<OrderItemPayload> restoredItems, String reason) implements Serializable {}
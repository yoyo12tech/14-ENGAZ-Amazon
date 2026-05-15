package com.team14.amazon.contracts.events;

import java.io.Serializable;

public record OrderCompletedEvent(Long orderId, Long userId, Long shippingAddressId, Double totalAmount) implements Serializable {}
package com.team14.amazon.contracts.events;
/** Consumed by billing to create a PENDING transaction when an order completes (S5-EVENTS). */
import java.io.Serializable;

public record OrderCompletedEvent(Long orderId, Long userId, Long shippingAddressId, Double totalAmount) implements Serializable {}


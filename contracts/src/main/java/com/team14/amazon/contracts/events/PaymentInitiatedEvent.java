package com.team14.amazon.contracts.events;

/** Published by billing when a saga payment is initialized (S5-EVENTS). */
public record PaymentInitiatedEvent(Long transactionId, Long orderId, Long userId, Double amount) {}

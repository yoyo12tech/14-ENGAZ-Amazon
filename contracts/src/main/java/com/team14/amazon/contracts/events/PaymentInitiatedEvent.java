package com.team14.amazon.contracts.events;

public record PaymentInitiatedEvent(Long transactionId, Long orderId, Long userId, Double amount) {}

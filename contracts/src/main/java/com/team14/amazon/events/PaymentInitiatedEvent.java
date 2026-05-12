package com.team14.amazon.events;

public record PaymentInitiatedEvent(Long transactionId, Long orderId, Long userId, Double amount) {}

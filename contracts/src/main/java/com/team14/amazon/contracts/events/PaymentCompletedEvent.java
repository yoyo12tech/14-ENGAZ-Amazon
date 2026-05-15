package com.team14.amazon.contracts.events;

public record PaymentCompletedEvent(Long transactionId, Long orderId, Long userId, Double amount) {}

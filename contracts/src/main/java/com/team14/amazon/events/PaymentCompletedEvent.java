package com.team14.amazon.events;

public record PaymentCompletedEvent(Long transactionId, Long orderId, Long userId, Double amount) {}

package com.team14.amazon.contracts.events;

public record PaymentFailedEvent(Long transactionId, Long orderId, String reason) {}

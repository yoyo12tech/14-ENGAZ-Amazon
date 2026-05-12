package com.team14.amazon.events;

public record PaymentFailedEvent(Long transactionId, Long orderId, String reason) {}

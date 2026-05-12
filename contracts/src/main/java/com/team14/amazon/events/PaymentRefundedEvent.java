package com.team14.amazon.events;

public record PaymentRefundedEvent(Long transactionId, Long orderId, Double refundAmount) {}

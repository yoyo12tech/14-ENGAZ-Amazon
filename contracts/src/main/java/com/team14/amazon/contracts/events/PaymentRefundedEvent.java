package com.team14.amazon.contracts.events;

public record PaymentRefundedEvent(Long transactionId, Long orderId, Double refundAmount) {}

package com.team14.amazon.contracts.events;

import java.io.Serializable;

public record PaymentRefundedEvent
        (Long transactionId, Long orderId, Double refundAmount) implements Serializable {}

package com.team14.amazon.contracts.events;

import java.io.Serializable;

public record PaymentCompletedEvent
        (Long transactionId, Long orderId, Long userId, Double amount) implements Serializable {}

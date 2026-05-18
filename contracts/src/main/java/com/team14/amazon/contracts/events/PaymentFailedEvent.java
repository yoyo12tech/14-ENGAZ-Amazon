package com.team14.amazon.contracts.events;

import java.io.Serializable;

public record PaymentFailedEvent
        (Long transactionId, Long orderId, String reason)implements Serializable {}

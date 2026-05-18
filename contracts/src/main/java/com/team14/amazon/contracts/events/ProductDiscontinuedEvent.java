package com.team14.amazon.contracts.events;

import java.io.Serializable;

public record ProductDiscontinuedEvent
        (Long productId, String oldStatus, String newStatus) implements Serializable {}

package com.team14.amazon.contracts.events;

public record ProductDiscontinuedEvent(Long productId, String oldStatus, String newStatus) {}

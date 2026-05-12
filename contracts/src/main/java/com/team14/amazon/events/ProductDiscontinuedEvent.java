package com.team14.amazon.events;

public record ProductDiscontinuedEvent(Long productId, String oldStatus, String newStatus) {}

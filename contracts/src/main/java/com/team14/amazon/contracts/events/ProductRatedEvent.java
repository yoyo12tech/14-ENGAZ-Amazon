package com.team14.amazon.contracts.events;

public record ProductRatedEvent(Long productId, Double newAverage, Integer totalRatings) {}

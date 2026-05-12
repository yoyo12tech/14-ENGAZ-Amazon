package com.team14.amazon.events;

public record ProductRatedEvent(Long productId, Double newAverage, Integer totalRatings) {}
